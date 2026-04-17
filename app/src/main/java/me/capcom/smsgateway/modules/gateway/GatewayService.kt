package me.capcom.smsgateway.modules.gateway

import android.content.Context
import android.os.Build
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import me.capcom.smsgateway.data.entities.MessageWithRecipients
import me.capcom.smsgateway.domain.EntitySource
import me.capcom.smsgateway.domain.MessageContent
import me.capcom.smsgateway.modules.events.EventBus
import me.capcom.smsgateway.modules.gateway.events.DeviceRegisteredEvent
import me.capcom.smsgateway.modules.gateway.services.SSEForegroundService
import me.capcom.smsgateway.modules.gateway.workers.PullMessagesWorker
import me.capcom.smsgateway.modules.gateway.workers.SendStateWorker
import me.capcom.smsgateway.modules.gateway.workers.SettingsUpdateWorker
import me.capcom.smsgateway.modules.gateway.workers.WebhooksUpdateWorker
import me.capcom.smsgateway.modules.logs.LogsService
import me.capcom.smsgateway.modules.logs.db.LogEntry
import me.capcom.smsgateway.modules.messages.MessagesService
import me.capcom.smsgateway.modules.messages.MessagesSettings
import me.capcom.smsgateway.modules.messages.data.SendParams
import me.capcom.smsgateway.modules.messages.data.SendRequest
import me.capcom.smsgateway.services.PushService
import java.util.Date

class GatewayService(
    private val context: Context,
    private val messagesService: MessagesService,
    private val settings: GatewaySettings,
    private val events: EventBus,
    private val logsService: LogsService,
) {
    private val eventsReceiver by lazy { EventsReceiver() }
    private val networkErrorHandler by lazy { NetworkErrorHandler(context) }

    private var _api: GatewayApi? = null

    companion object {
        private const val MODULE_NAME = "GatewayService"
    }

    private val api
        get() = _api ?: GatewayApi(
            settings.serverUrl,
            settings.privateToken
        ).also { _api = it }

    //region Start, stop, etc...
    fun start(context: Context) {
        if (!settings.enabled) return
        
        // تسجيل بدء الخدمة
        logsService.insert(
            LogEntry.Priority.INFO,
            MODULE_NAME,
            "بدء تشغيل خدمة Gateway - الوضع: ${settings.serverMode}"
        )

        // تشغيل الخدمات حسب الوضع المحدد
        when (settings.serverMode) {
            GatewaySettings.ServerMode.LOCAL_ONLY -> {
                logsService.insert(
                    LogEntry.Priority.INFO,
                    MODULE_NAME,
                    "تشغيل الوضع المحلي فقط - تم تخطي خدمات الخادم السحابي"
                )
                // لا نحتاج PushService أو Workers في الوضع المحلي
            }
            GatewaySettings.ServerMode.CLOUD_ONLY,
            GatewaySettings.ServerMode.AUTO -> {
                try {
                    PushService.register(context)
                    PullMessagesWorker.start(context)
                    WebhooksUpdateWorker.start(context)
                    SettingsUpdateWorker.start(context)
                    
                    logsService.insert(
                        LogEntry.Priority.INFO,
                        MODULE_NAME,
                        "تم تشغيل خدمات الخادم السحابي بنجاح"
                    )
                } catch (e: Exception) {
                    logsService.insert(
                        LogEntry.Priority.ERROR,
                        MODULE_NAME,
                        "فشل في تشغيل خدمات الخادم السحابي: ${e.message}",
                        mapOf("error" to e.toString())
                    )
                    
                    // في حالة الوضع التلقائي، التبديل إلى المحلي
                    if (settings.serverMode == GatewaySettings.ServerMode.AUTO) {
                        settings.serverMode = GatewaySettings.ServerMode.LOCAL_ONLY
                        logsService.insert(
                            LogEntry.Priority.WARN,
                            MODULE_NAME,
                            "تم التبديل إلى الوضع المحلي بسبب فشل الاتصال بالخادم السحابي"
                        )
                    }
                }
            }
        }

        eventsReceiver.start()
    }

    fun stop(context: Context) {
        eventsReceiver.stop()

        SSEForegroundService.stop(context)
        SettingsUpdateWorker.stop(context)
        WebhooksUpdateWorker.stop(context)
        PullMessagesWorker.stop(context)

        this._api = null
    }

    fun isActiveLiveData(context: Context) = PullMessagesWorker.getStateLiveData(context)
    
    /**
     * اختبار الاتصال بالخادم السحابي
     */
    suspend fun testCloudConnection(): Boolean {
        return try {
            if (!settings.isCloudEnabled) {
                logsService.insert(
                    LogEntry.Priority.INFO,
                    MODULE_NAME,
                    "اختبار الاتصال متخطى - الوضع المحلي مفعل"
                )
                return false
            }
            
            logsService.insert(
                LogEntry.Priority.INFO,
                MODULE_NAME,
                "بدء اختبار الاتصال بالخادم السحابي: ${settings.serverUrl}"
            )
            
            val response = api.getDevice(settings.registrationInfo?.token)
            
            logsService.insert(
                LogEntry.Priority.INFO,
                MODULE_NAME,
                "نجح اختبار الاتصال بالخادم السحابي - IP الخارجي: ${response.externalIp}"
            )
            
            true
        } catch (e: Exception) {
            logsService.insert(
                LogEntry.Priority.ERROR,
                MODULE_NAME,
                "فشل اختبار الاتصال بالخادم السحابي: ${e.message}",
                mapOf(
                    "server_url" to settings.serverUrl,
                    "error" to e.toString()
                )
            )
            false
        }
    }
    
    /**
     * الحصول على حالة الاتصال الحالية
     */
    fun getConnectionStatus(): ConnectionStatus {
        return when {
            !settings.enabled -> ConnectionStatus.DISABLED
            settings.serverMode == GatewaySettings.ServerMode.LOCAL_ONLY -> ConnectionStatus.LOCAL_ONLY
            settings.registrationInfo != null -> ConnectionStatus.CLOUD_CONNECTED
            else -> ConnectionStatus.CLOUD_DISCONNECTED
        }
    }
    
    enum class ConnectionStatus {
        DISABLED,           // الخدمة معطلة
        LOCAL_ONLY,         // الوضع المحلي فقط
        CLOUD_CONNECTED,    // متصل بالخادم السحابي
        CLOUD_DISCONNECTED  // غير متصل بالخادم السحابي
    }
    //endregion

    //region Account
    suspend fun getLoginCode(): GatewayApi.GetUserCodeResponse {
        val username = settings.username
            ?: throw IllegalStateException("Username is not set")
        val password = settings.password
            ?: throw IllegalStateException("Password is not set")

        return api.getUserCode(username to password)
    }

    suspend fun changePassword(current: String, new: String) {
        val info = settings.registrationInfo
            ?: throw IllegalStateException("The device is not registered on the server")

        this.api.changeUserPassword(
            info.token,
            GatewayApi.PasswordChangeRequest(current, new)
        )

        settings.registrationInfo = info.copy(password = new)

        events.emit(
            DeviceRegisteredEvent.Success(
                api.hostname,
                info.login,
                new,
            )
        )
    }
    //endregion

     //region Device
    internal suspend fun registerDevice(
        pushToken: String?,
        registerMode: RegistrationMode
    ) {
        if (!settings.enabled) return

        val settings = settings.registrationInfo
        val accessToken = settings?.token

        if (accessToken != null) {
            // if there's an access token, try to update push token
            try {
                updateDevice(pushToken)
                return
            } catch (e: ClientRequestException) {
                // if token is invalid, try to register new one
                if (e.response.status != HttpStatusCode.Unauthorized) {
                    throw e
                }
            }
        }

        try {
            val deviceName = "${Build.MANUFACTURER}/${Build.PRODUCT}"
            val request = GatewayApi.DeviceRegisterRequest(
                deviceName,
                pushToken
            )
            val response = when (registerMode) {
                RegistrationMode.Anonymous -> api.deviceRegister(request, null)
                is RegistrationMode.WithCode -> api.deviceRegister(request, registerMode.code)
                is RegistrationMode.WithCredentials -> api.deviceRegister(
                    request,
                    registerMode.login to registerMode.password
                )
            }

            this.settings.fcmToken = pushToken
            this.settings.registrationInfo = response

            events.emit(
                DeviceRegisteredEvent.Success(
                    api.hostname,
                    response.login,
                    response.password,
                )
            )
        } catch (th: Throwable) {
            val errorMessage = networkErrorHandler.handleNetworkError(th)
            logsService.insert(
                LogEntry.Priority.ERROR,
                MODULE_NAME,
                "فشل في تسجيل الجهاز: $errorMessage",
                mapOf(
                    "error_type" to th.javaClass.simpleName,
                    "error_message" to th.message,
                    "server_url" to this.settings.serverUrl,
                    "network_available" to networkErrorHandler.isNetworkAvailable(),
                    "registration_mode" to registerMode.javaClass.simpleName
                )
            )
            
            events.emit(
                DeviceRegisteredEvent.Failure(
                    api.hostname,
                    errorMessage
                )
            )

            throw th
        }
    }

    internal suspend fun updateDevice(pushToken: String?) {
        if (!settings.enabled) return

        val settings = settings.registrationInfo ?: return
        val accessToken = settings.token

        api.devicePatch(
            accessToken,
            GatewayApi.DevicePatchRequest(
                settings.id,
                pushToken
            )
        )

        this.settings.fcmToken = pushToken

        events.emit(
            DeviceRegisteredEvent.Success(
                api.hostname,
                settings.login,
                settings.password,
            )
        )
    }

    sealed class RegistrationMode {
        object Anonymous : RegistrationMode()
        class WithCredentials(val login: String, val password: String) : RegistrationMode()
        class WithCode(val code: String) : RegistrationMode()
    }
    //endregion

    //region Messages
    internal suspend fun getNewMessages(context: Context) {
        if (!settings.enabled) return
        val settings = settings.registrationInfo ?: return
        val processingOrder = when (messagesService.processingOrder) {
            MessagesSettings.ProcessingOrder.LIFO -> GatewayApi.ProcessingOrder.LIFO
            MessagesSettings.ProcessingOrder.FIFO -> GatewayApi.ProcessingOrder.FIFO
        }
        val messages = api.getMessages(settings.token, processingOrder)
        for (message in messages) {
            try {
                processMessage(context, message)
            } catch (th: Throwable) {
                logsService.insert(
                    LogEntry.Priority.ERROR,
                    MODULE_NAME,
                    "Failed to process message",
                    mapOf(
                        "message" to message,
                        "exception" to th.stackTraceToString(),
                    )
                )
                th.printStackTrace()
            }
        }
    }

    private fun processMessage(context: Context, message: GatewayApi.Message) {
        val messageState = messagesService.getMessage(message.id)
        if (messageState != null) {
            SendStateWorker.start(context, message.id)
            return
        }

        val request = SendRequest(
            EntitySource.Cloud,
            me.capcom.smsgateway.modules.messages.data.Message(
                message.id,
                when (val content = message.content) {
                    is GatewayApi.MessageContent.Text -> MessageContent.Text(content.text)
                    is GatewayApi.MessageContent.Data -> MessageContent.Data(
                        content.data,
                        content.port
                    )
                },
                message.phoneNumbers,
                message.isEncrypted ?: false,
                message.createdAt ?: Date(),
            ),
            SendParams(
                message.withDeliveryReport ?: true,
                skipPhoneValidation = true,
                simNumber = message.simNumber,
                validUntil = message.validUntil,
                priority = message.priority,
            )
        )
        messagesService.enqueueMessage(request)
    }

    internal suspend fun sendState(
        message: MessageWithRecipients
    ) {
        val settings = settings.registrationInfo ?: return

        api.patchMessages(
            settings.token,
            listOf(
                GatewayApi.MessagePatchRequest(
                    message.message.id,
                    message.message.state,
                    message.recipients.map {
                        GatewayApi.RecipientState(
                            it.phoneNumber,
                            it.state,
                            it.error
                        )
                    },
                    message.states.associate { it.state to Date(it.updatedAt) }
                )
            )
        )
    }
    //endregion

    //region Webhooks
    internal suspend fun getWebHooks(): List<GatewayApi.WebHook> {
        val settings = settings.registrationInfo
        return if (settings != null) {
            api.getWebHooks(settings.token)
        } else {
            emptyList()
        }
    }
    //endregion

    //region Settings
    internal suspend fun getSettings(): Map<String, *>? {
        val settings = settings.registrationInfo ?: return null

        return api.getSettings(settings.token)
    }
    //endregion

    //region Utility
    suspend fun testConnection(): Boolean {
        return try {
            if (!networkErrorHandler.isNetworkAvailable()) {
                logsService.insert(
                    LogEntry.Priority.ERROR,
                    MODULE_NAME,
                    "لا يوجد اتصال بالإنترنت",
                    mapOf(
                        "network_type" to networkErrorHandler.getNetworkType(),
                        "server_url" to settings.serverUrl
                    )
                )
                return false
            }
            
            val response = api.getDevice(settings.registrationInfo?.token)
            logsService.insert(
                LogEntry.Priority.INFO,
                MODULE_NAME,
                "تم الاتصال بالخادم السحابي بنجاح",
                mapOf(
                    "server_url" to settings.serverUrl,
                    "external_ip" to response.externalIp,
                    "network_type" to networkErrorHandler.getNetworkType()
                )
            )
            true
        } catch (e: Exception) {
            val errorMessage = networkErrorHandler.handleNetworkError(e)
            logsService.insert(
                LogEntry.Priority.ERROR,
                MODULE_NAME,
                "فشل في الاتصال بالخادم السحابي: $errorMessage",
                mapOf(
                    "error_type" to e.javaClass.simpleName,
                    "error_message" to e.message,
                    "server_url" to settings.serverUrl,
                    "network_type" to networkErrorHandler.getNetworkType(),
                    "network_available" to networkErrorHandler.isNetworkAvailable()
                )
            )
            false
        }
    }

    suspend fun getPublicIP(): String {
        return GatewayApi(
            settings.serverUrl,
            settings.privateToken
        )
            .getDevice(settings.registrationInfo?.token)
            .externalIp
    }
    //endregion
}
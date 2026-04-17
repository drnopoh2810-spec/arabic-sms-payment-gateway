package me.capcom.smsgateway.modules.gateway

import me.capcom.smsgateway.modules.settings.Exporter
import me.capcom.smsgateway.modules.settings.Importer
import me.capcom.smsgateway.modules.settings.KeyValueStorage
import me.capcom.smsgateway.modules.settings.get

class GatewaySettings(
    private val storage: KeyValueStorage,
) : Exporter, Importer {
    enum class NotificationChannel {
        AUTO,
        SSE_ONLY,
    }
    
    enum class ServerMode {
        LOCAL_ONLY,    // الوضع المحلي فقط
        CLOUD_ONLY,    // الخادم السحابي فقط  
        AUTO           // تلقائي (محلي أولاً، ثم سحابي)
    }

    var enabled: Boolean
        get() = storage.get<Boolean>(ENABLED) ?: false
        set(value) = storage.set(ENABLED, value)
        
    var serverMode: ServerMode
        get() = storage.get<ServerMode>(SERVER_MODE) ?: ServerMode.LOCAL_ONLY
        set(value) = storage.set(SERVER_MODE, value)

    val deviceId: String?
        get() = registrationInfo?.id

    var registrationInfo: GatewayApi.DeviceRegisterResponse?
        get() = storage.get(REGISTRATION_INFO)
        set(value) = storage.set(REGISTRATION_INFO, value)

    var fcmToken: String?
        get() = storage.get(FCM_TOKEN)
        set(value) = storage.set(FCM_TOKEN, value)

    val username: String?
        get() = registrationInfo?.login
    val password: String?
        get() = registrationInfo?.password

    val serverUrl: String
        get() = when (serverMode) {
            ServerMode.LOCAL_ONLY -> "http://localhost:8080"
            ServerMode.CLOUD_ONLY -> storage.get<String?>(CLOUD_URL) ?: PUBLIC_URL
            ServerMode.AUTO -> storage.get<String?>(CLOUD_URL) ?: PUBLIC_URL
        }
        
    val isCloudEnabled: Boolean
        get() = serverMode != ServerMode.LOCAL_ONLY
        
    val privateToken: String?
        get() = storage.get<String>(PRIVATE_TOKEN)

    val notificationChannel: NotificationChannel
        get() = storage.get<NotificationChannel>(NOTIFICATION_CHANNEL) ?: NotificationChannel.AUTO

    companion object {
        private const val REGISTRATION_INFO = "REGISTRATION_INFO"
        private const val ENABLED = "ENABLED"
        private const val FCM_TOKEN = "fcm_token"
        private const val SERVER_MODE = "server_mode"

        private const val CLOUD_URL = "cloud_url"
        private const val PRIVATE_TOKEN = "private_token"
        private const val NOTIFICATION_CHANNEL = "notification_channel"

        const val PUBLIC_URL = "https://api.sms-gate.app/mobile/v1"
    }

    override fun export(): Map<String, *> {
        return mapOf(
            CLOUD_URL to (if (serverMode != ServerMode.LOCAL_ONLY) serverUrl else null),
            SERVER_MODE to serverMode.name,
            NOTIFICATION_CHANNEL to notificationChannel.name,
            PRIVATE_TOKEN to privateToken
        )
    }

    override fun import(data: Map<String, *>): Boolean {
        return data.map {
            when (it.key) {
                CLOUD_URL -> {
                    val url = it.value?.toString()
                    if (url != null && !url.startsWith("https://")) {
                        throw IllegalArgumentException("url must start with https://")
                    }

                    val changed = (storage.get<String?>(CLOUD_URL) ?: PUBLIC_URL) != url

                    storage.set(it.key, url)

                    changed
                }

                SERVER_MODE -> {
                    val newValue = it.value?.let { ServerMode.valueOf(it.toString()) }
                        ?: ServerMode.LOCAL_ONLY
                    val changed = serverMode != newValue

                    storage.set(it.key, newValue)

                    changed
                }

                PRIVATE_TOKEN -> {
                    val newValue = it.value?.toString()
                    val changed = privateToken != newValue

                    storage.set(it.key, newValue)

                    changed
                }

                NOTIFICATION_CHANNEL -> {
                    val newValue = it.value?.let { NotificationChannel.valueOf(it.toString()) }
                        ?: NotificationChannel.AUTO
                    val changed = notificationChannel != newValue

                    storage.set(it.key, newValue)

                    changed
                }

                else -> false
            }
        }.any { it }
    }
}
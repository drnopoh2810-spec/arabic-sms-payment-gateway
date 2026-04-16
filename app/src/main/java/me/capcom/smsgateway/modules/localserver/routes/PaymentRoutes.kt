package me.capcom.smsgateway.modules.localserver.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import me.capcom.smsgateway.modules.localserver.auth.requireScope
import me.capcom.smsgateway.modules.localserver.auth.AuthScopes
import me.capcom.smsgateway.modules.payment.PaymentService
import me.capcom.smsgateway.modules.payment.PaymentSettings
import me.capcom.smsgateway.modules.payment.PaymentWalletType
import me.capcom.smsgateway.modules.payment.data.PaymentSettingsRequest
import me.capcom.smsgateway.modules.payment.data.PaymentStatsResponse
import me.capcom.smsgateway.modules.payment.data.PaymentTransactionResponse
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

fun Route.paymentRoutes() {
    val paymentService by inject<PaymentService>(PaymentService::class.java)
    val paymentSettings by inject<PaymentSettings>(PaymentSettings::class.java)

    route("/payments") {
        authenticate("auth-basic", "auth-jwt") {
            
            // Get payment transactions
            get {
                requireScope(AuthScopes.MessagesRead)
                
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val walletType = call.request.queryParameters["wallet_type"]?.let { 
                    try {
                        PaymentWalletType.valueOf(it.uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                
                val transactions = if (walletType != null) {
                    paymentService.getTransactionsByWallet(walletType, limit)
                } else {
                    paymentService.getRecentTransactions(limit).value ?: emptyList()
                }
                
                val response = transactions.map { transaction ->
                    PaymentTransactionResponse(
                        id = transaction.id,
                        messageId = transaction.messageId,
                        walletType = transaction.walletType.name,
                        amount = transaction.amount,
                        currency = transaction.currency,
                        senderName = transaction.senderName,
                        senderPhone = transaction.senderPhone,
                        reference = transaction.reference,
                        transactionId = transaction.transactionId,
                        description = transaction.description,
                        isConfirmed = transaction.isConfirmed,
                        isProcessed = transaction.isProcessed,
                        createdAt = transaction.createdAt,
                        confirmedAt = transaction.confirmedAt,
                        processedAt = transaction.processedAt
                    )
                }
                
                call.respond(mapOf("transactions" to response))
            }
            
            // Get specific transaction
            get("/{id}") {
                requireScope(AuthScopes.MessagesRead)
                
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, 
                    mapOf("error" to "Transaction ID is required")
                )
                
                val transaction = paymentService.getTransactionById(id)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Transaction not found")
                    )
                
                val response = PaymentTransactionResponse(
                    id = transaction.id,
                    messageId = transaction.messageId,
                    walletType = transaction.walletType.name,
                    amount = transaction.amount,
                    currency = transaction.currency,
                    senderName = transaction.senderName,
                    senderPhone = transaction.senderPhone,
                    reference = transaction.reference,
                    transactionId = transaction.transactionId,
                    description = transaction.description,
                    isConfirmed = transaction.isConfirmed,
                    isProcessed = transaction.isProcessed,
                    createdAt = transaction.createdAt,
                    confirmedAt = transaction.confirmedAt,
                    processedAt = transaction.processedAt
                )
                
                call.respond(response)
            }
            
            // Confirm payment
            post("/{id}/confirm") {
                requireScope(AuthScopes.MessagesWrite)
                
                val id = call.parameters["id"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Transaction ID is required")
                )
                
                val success = paymentService.confirmPayment(id)
                
                if (success) {
                    call.respond(mapOf("success" to true, "message" to "Payment confirmed"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Transaction not found or already confirmed")
                    )
                }
            }
            
            // Get payment statistics
            get("/stats") {
                requireScope(AuthScopes.MessagesRead)
                
                val hours = call.request.queryParameters["hours"]?.toIntOrNull() ?: 24
                val fromTime = System.currentTimeMillis() - (hours * 3600 * 1000L)
                
                val stats = paymentService.getPaymentStats(fromTime)
                
                val response = PaymentStatsResponse(
                    confirmedTransactions = stats.confirmedTransactions,
                    totalAmount = stats.totalAmount.toString(),
                    currency = stats.currency,
                    periodHours = hours
                )
                
                call.respond(response)
            }
            
            // Get pending transactions
            get("/pending") {
                requireScope(AuthScopes.MessagesRead)
                
                val pendingTransactions = paymentService.getPendingTransactions()
                
                val response = pendingTransactions.map { transaction ->
                    PaymentTransactionResponse(
                        id = transaction.id,
                        messageId = transaction.messageId,
                        walletType = transaction.walletType.name,
                        amount = transaction.amount,
                        currency = transaction.currency,
                        senderName = transaction.senderName,
                        senderPhone = transaction.senderPhone,
                        reference = transaction.reference,
                        transactionId = transaction.transactionId,
                        description = transaction.description,
                        isConfirmed = transaction.isConfirmed,
                        isProcessed = transaction.isProcessed,
                        createdAt = transaction.createdAt,
                        confirmedAt = transaction.confirmedAt,
                        processedAt = transaction.processedAt
                    )
                }
                
                call.respond(mapOf("transactions" to response))
            }
        }
        
        // Payment settings endpoints
        route("/settings") {
            authenticate("basic", "jwt") {
                
                // Get payment settings
                get {
                    requireScope(AuthScopes.SettingsRead)
                    
                    val response = mapOf(
                        "enabled" to paymentSettings.isEnabled,
                        "auto_confirm" to paymentSettings.autoConfirmPayments,
                        "show_notifications" to paymentSettings.showPaymentNotifications,
                        "webhook_url" to paymentSettings.paymentWebhookUrl,
                        "enabled_wallets" to paymentSettings.enabledWalletTypes.map { it.name },
                        "minimum_amount" to paymentSettings.minimumAmount,
                        "maximum_amount" to paymentSettings.maximumAmount,
                        "retention_days" to paymentSettings.retentionDays,
                        "require_confirmation" to paymentSettings.requireConfirmation,
                        "webhook_timeout" to paymentSettings.webhookTimeout,
                        "webhook_retries" to paymentSettings.webhookRetries
                    )
                    
                    call.respond(response)
                }
                
                // Update payment settings
                patch {
                    requireScope(AuthScopes.SettingsWrite)
                    
                    val request = call.receive<PaymentSettingsRequest>()
                    
                    request.enabled?.let { paymentSettings.isEnabled = it }
                    request.autoConfirm?.let { paymentSettings.autoConfirmPayments = it }
                    request.showNotifications?.let { paymentSettings.showPaymentNotifications = it }
                    request.webhookUrl?.let { paymentSettings.paymentWebhookUrl = it }
                    request.enabledWallets?.let { wallets ->
                        val walletTypes = wallets.mapNotNull { walletName ->
                            try {
                                PaymentWalletType.valueOf(walletName.uppercase())
                            } catch (e: IllegalArgumentException) {
                                null
                            }
                        }.toSet()
                        paymentSettings.enabledWalletTypes = walletTypes
                    }
                    request.minimumAmount?.let { 
                        // Validate amount format
                        try {
                            BigDecimal(it)
                            paymentSettings.minimumAmount = it
                        } catch (e: NumberFormatException) {
                            return@patch call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid minimum amount format")
                            )
                        }
                    }
                    request.maximumAmount?.let { 
                        if (it.isNotBlank()) {
                            try {
                                BigDecimal(it)
                                paymentSettings.maximumAmount = it
                            } catch (e: NumberFormatException) {
                                return@patch call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("error" to "Invalid maximum amount format")
                                )
                            }
                        } else {
                            paymentSettings.maximumAmount = null
                        }
                    }
                    request.retentionDays?.let { 
                        if (it in 1..365) {
                            paymentSettings.retentionDays = it
                        } else {
                            return@patch call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Retention days must be between 1 and 365")
                            )
                        }
                    }
                    request.requireConfirmation?.let { paymentSettings.requireConfirmation = it }
                    request.webhookTimeout?.let { 
                        if (it in 5..300) {
                            paymentSettings.webhookTimeout = it
                        } else {
                            return@patch call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Webhook timeout must be between 5 and 300 seconds")
                            )
                        }
                    }
                    request.webhookRetries?.let { 
                        if (it in 0..10) {
                            paymentSettings.webhookRetries = it
                        } else {
                            return@patch call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Webhook retries must be between 0 and 10")
                            )
                        }
                    }
                    
                    call.respond(mapOf("success" to true, "message" to "Settings updated"))
                }
            }
        }
    }
}
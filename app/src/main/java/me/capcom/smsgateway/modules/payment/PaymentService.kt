package me.capcom.smsgateway.modules.payment

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.capcom.smsgateway.data.dao.PaymentTransactionsDao
import me.capcom.smsgateway.data.entities.PaymentTransaction
import me.capcom.smsgateway.modules.events.EventBus
import me.capcom.smsgateway.modules.payment.data.PaymentInfo
import me.capcom.smsgateway.modules.payment.events.PaymentDetectedEvent
import me.capcom.smsgateway.modules.payment.events.PaymentConfirmedEvent
import me.capcom.smsgateway.modules.webhooks.WebHooksService
import me.capcom.smsgateway.modules.webhooks.domain.WebHookEvent
import me.capcom.smsgateway.modules.notifications.NotificationsService
import java.math.BigDecimal

class PaymentService(
    private val context: Context,
    private val dao: PaymentTransactionsDao,
    private val parser: PaymentParser,
    private val events: EventBus,
    private val webhooksService: WebHooksService,
    private val notificationsService: NotificationsService,
    private val settings: PaymentSettings
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "PaymentService"
    }
    
    fun processIncomingMessage(messageText: String, sender: String, messageId: String) {
        scope.launch {
            try {
                if (!parser.isPaymentMessage(messageText, sender)) {
                    return@launch
                }
                
                val paymentInfo = parser.parsePayment(messageText, sender, messageId)
                if (paymentInfo != null) {
                    handlePaymentDetected(paymentInfo)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment message", e)
            }
        }
    }
    
    private suspend fun handlePaymentDetected(paymentInfo: PaymentInfo) {
        // Check if already processed
        val existing = dao.getByMessageId(paymentInfo.messageId)
        if (existing != null) {
            Log.d(TAG, "Payment already processed for message ${paymentInfo.messageId}")
            return
        }
        
        // Store payment transaction
        val transaction = PaymentTransaction(
            id = paymentInfo.id,
            messageId = paymentInfo.messageId,
            walletType = paymentInfo.walletType,
            amount = paymentInfo.amount.toString(),
            currency = paymentInfo.currency,
            senderName = paymentInfo.senderName,
            senderPhone = paymentInfo.senderPhone,
            reference = paymentInfo.reference,
            transactionId = paymentInfo.transactionId,
            description = paymentInfo.description,
            rawMessage = paymentInfo.rawMessage,
            webhookUrl = settings.paymentWebhookUrl
        )
        
        dao.insert(transaction)
        
        // Emit event
        events.emit(PaymentDetectedEvent(paymentInfo))
        
        // Send notification if enabled
        if (settings.showPaymentNotifications) {
            showPaymentNotification(paymentInfo)
        }
        
        // Auto-confirm if enabled
        if (settings.autoConfirmPayments) {
            confirmPayment(transaction.id)
        }
        
        // Send webhook if configured
        if (!settings.paymentWebhookUrl.isNullOrBlank()) {
            sendPaymentWebhook(transaction)
        }
        
        Log.i(TAG, "Payment detected: ${paymentInfo.amount} ${paymentInfo.currency} from ${paymentInfo.walletType}")
    }
    
    fun confirmPayment(transactionId: String): Boolean {
        return try {
            val transaction = dao.get(transactionId) ?: return false
            
            dao.confirmTransaction(transactionId, System.currentTimeMillis())
            
            val updatedTransaction = dao.get(transactionId)!!
            events.emit(PaymentConfirmedEvent(updatedTransaction))
            
            // Process confirmed payment
            if (!settings.paymentWebhookUrl.isNullOrBlank()) {
                scope.launch {
                    processConfirmedPayment(updatedTransaction)
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error confirming payment", e)
            false
        }
    }
    
    private suspend fun processConfirmedPayment(transaction: PaymentTransaction) {
        try {
            val webhookData = mapOf(
                "event" to "payment_confirmed",
                "transaction_id" to transaction.id,
                "wallet_type" to transaction.walletType.name,
                "amount" to transaction.amount,
                "currency" to transaction.currency,
                "sender_name" to transaction.senderName,
                "sender_phone" to transaction.senderPhone,
                "reference" to transaction.reference,
                "transaction_ref" to transaction.transactionId,
                "description" to transaction.description,
                "confirmed_at" to transaction.confirmedAt,
                "created_at" to transaction.createdAt
            )
            
            val response = webhooksService.emit(
                context = context,
                event = WebHookEvent.PaymentConfirmed,
                payload = webhookData
            )
            
            dao.markProcessed(
                id = transaction.id,
                processedAt = System.currentTimeMillis(),
                response = "webhook_sent"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing confirmed payment webhook", e)
        }
    }
    
    private suspend fun sendPaymentWebhook(transaction: PaymentTransaction) {
        try {
            val webhookData = mapOf(
                "event" to "payment_detected",
                "transaction_id" to transaction.id,
                "wallet_type" to transaction.walletType.name,
                "amount" to transaction.amount,
                "currency" to transaction.currency,
                "sender_name" to transaction.senderName,
                "sender_phone" to transaction.senderPhone,
                "reference" to transaction.reference,
                "transaction_ref" to transaction.transactionId,
                "description" to transaction.description,
                "created_at" to transaction.createdAt,
                "raw_message" to transaction.rawMessage
            )
            
            webhooksService.emit(
                context = context,
                event = WebHookEvent.PaymentDetected,
                payload = webhookData
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending payment webhook", e)
        }
    }
    
    private fun showPaymentNotification(paymentInfo: PaymentInfo) {
        val title = "تم اكتشاف عملية دفع"
        val message = "${paymentInfo.amount} ${paymentInfo.currency} من ${paymentInfo.walletType.displayName}"
        
        notificationsService.showPaymentNotification(
            title = title,
            message = message,
            transactionId = paymentInfo.id
        )
    }
    
    fun getPendingTransactions() = dao.getPendingTransactions()
    
    fun getTransactionById(id: String) = dao.get(id)
    
    fun getRecentTransactions(limit: Int = 50) = dao.selectLast(limit)
    
    fun getTransactionsByWallet(walletType: PaymentWalletType, limit: Int = 20) = 
        dao.getByWalletType(walletType, limit)
    
    fun getPaymentStats(fromTime: Long): PaymentStats {
        val confirmedCount = dao.countConfirmedFrom(fromTime)
        val totalAmount = dao.sumConfirmedAmountFrom(fromTime) ?: 0.0
        
        return PaymentStats(
            confirmedTransactions = confirmedCount,
            totalAmount = BigDecimal.valueOf(totalAmount),
            currency = "EGP"
        )
    }
}

data class PaymentStats(
    val confirmedTransactions: Int,
    val totalAmount: BigDecimal,
    val currency: String
)
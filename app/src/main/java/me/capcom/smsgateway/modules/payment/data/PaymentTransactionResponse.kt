package me.capcom.smsgateway.modules.payment.data

import java.util.Date

data class PaymentTransactionResponse(
    val id: String,
    val messageId: String,
    val walletType: String,
    val amount: String,
    val currency: String,
    val senderName: String?,
    val senderPhone: String?,
    val reference: String?,
    val transactionId: String?,
    val description: String?,
    val isConfirmed: Boolean,
    val isProcessed: Boolean,
    val createdAt: Date,
    val confirmedAt: Date?,
    val processedAt: Date?
)
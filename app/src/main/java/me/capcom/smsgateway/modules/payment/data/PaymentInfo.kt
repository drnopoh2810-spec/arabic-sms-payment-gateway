package me.capcom.smsgateway.modules.payment.data

import me.capcom.smsgateway.modules.payment.PaymentWalletType
import java.math.BigDecimal
import java.util.Date

data class PaymentInfo(
    val id: String,
    val walletType: PaymentWalletType,
    val amount: BigDecimal,
    val currency: String = "EGP",
    val senderName: String?,
    val senderPhone: String?,
    val reference: String?,
    val transactionId: String?,
    val description: String?,
    val timestamp: Date,
    val rawMessage: String,
    val messageId: String,
    val isConfirmed: Boolean = false
)

data class PaymentPattern(
    val walletType: PaymentWalletType,
    val amountRegex: Regex,
    val senderRegex: Regex?,
    val referenceRegex: Regex?,
    val transactionIdRegex: Regex?
)
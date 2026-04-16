package me.capcom.smsgateway.modules.payment.events

import me.capcom.smsgateway.data.entities.PaymentTransaction
import me.capcom.smsgateway.modules.payment.data.PaymentInfo

data class PaymentDetectedEvent(
    val paymentInfo: PaymentInfo
)

data class PaymentConfirmedEvent(
    val transaction: PaymentTransaction
)

data class PaymentProcessedEvent(
    val transaction: PaymentTransaction,
    val success: Boolean,
    val response: String?
)
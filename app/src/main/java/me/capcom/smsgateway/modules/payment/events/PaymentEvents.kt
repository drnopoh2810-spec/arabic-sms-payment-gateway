package me.capcom.smsgateway.modules.payment.events

import me.capcom.smsgateway.data.entities.PaymentTransaction
import me.capcom.smsgateway.modules.events.AppEvent
import me.capcom.smsgateway.modules.payment.data.PaymentInfo

data class PaymentDetectedEvent(
    val paymentInfo: PaymentInfo
) : AppEvent("payment_detected")

data class PaymentConfirmedEvent(
    val transaction: PaymentTransaction
) : AppEvent("payment_confirmed")

data class PaymentProcessedEvent(
    val transaction: PaymentTransaction,
    val success: Boolean,
    val response: String?
) : AppEvent("payment_processed")
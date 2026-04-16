package me.capcom.smsgateway.modules.payment.events

import me.capcom.smsgateway.modules.events.AppEvent
import me.capcom.smsgateway.modules.payment.data.PaymentInfo

class PaymentDetectedEvent(
    val paymentInfo: PaymentInfo
) : AppEvent("payment_detected")
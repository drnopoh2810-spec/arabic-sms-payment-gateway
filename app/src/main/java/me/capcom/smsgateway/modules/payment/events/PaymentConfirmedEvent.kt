package me.capcom.smsgateway.modules.payment.events

import me.capcom.smsgateway.modules.events.AppEvent
import me.capcom.smsgateway.data.entities.PaymentTransaction

class PaymentConfirmedEvent(
    val transaction: PaymentTransaction
) : AppEvent("payment_confirmed")
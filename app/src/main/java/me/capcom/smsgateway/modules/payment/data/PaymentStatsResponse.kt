package me.capcom.smsgateway.modules.payment.data

data class PaymentStatsResponse(
    val confirmedTransactions: Int,
    val totalAmount: String,
    val currency: String,
    val periodHours: Int
)
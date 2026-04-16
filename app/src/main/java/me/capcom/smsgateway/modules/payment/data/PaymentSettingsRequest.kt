package me.capcom.smsgateway.modules.payment.data

data class PaymentSettingsRequest(
    val enabled: Boolean? = null,
    val autoConfirm: Boolean? = null,
    val showNotifications: Boolean? = null,
    val webhookUrl: String? = null,
    val enabledWallets: List<String>? = null,
    val minimumAmount: String? = null,
    val maximumAmount: String? = null,
    val retentionDays: Int? = null,
    val requireConfirmation: Boolean? = null,
    val webhookTimeout: Int? = null,
    val webhookRetries: Int? = null
)
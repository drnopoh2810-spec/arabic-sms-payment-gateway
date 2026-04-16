package me.capcom.smsgateway.modules.payment.data

import com.google.gson.annotations.SerializedName

data class PaymentTransactionResponse(
    val id: String,
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("wallet_type")
    val walletType: String,
    val amount: String,
    val currency: String,
    @SerializedName("sender_name")
    val senderName: String?,
    @SerializedName("sender_phone")
    val senderPhone: String?,
    val reference: String?,
    @SerializedName("transaction_id")
    val transactionId: String?,
    val description: String?,
    @SerializedName("is_confirmed")
    val isConfirmed: Boolean,
    @SerializedName("is_processed")
    val isProcessed: Boolean,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("confirmed_at")
    val confirmedAt: Long?,
    @SerializedName("processed_at")
    val processedAt: Long?
)

data class PaymentConfirmRequest(
    @SerializedName("transaction_id")
    val transactionId: String
)

data class PaymentStatsResponse(
    @SerializedName("confirmed_transactions")
    val confirmedTransactions: Int,
    @SerializedName("total_amount")
    val totalAmount: String,
    val currency: String,
    @SerializedName("period_hours")
    val periodHours: Int
)

data class PaymentSettingsRequest(
    val enabled: Boolean?,
    @SerializedName("auto_confirm")
    val autoConfirm: Boolean?,
    @SerializedName("show_notifications")
    val showNotifications: Boolean?,
    @SerializedName("webhook_url")
    val webhookUrl: String?,
    @SerializedName("enabled_wallets")
    val enabledWallets: List<String>?,
    @SerializedName("minimum_amount")
    val minimumAmount: String?,
    @SerializedName("maximum_amount")
    val maximumAmount: String?,
    @SerializedName("retention_days")
    val retentionDays: Int?,
    @SerializedName("require_confirmation")
    val requireConfirmation: Boolean?,
    @SerializedName("webhook_timeout")
    val webhookTimeout: Int?,
    @SerializedName("webhook_retries")
    val webhookRetries: Int?
)
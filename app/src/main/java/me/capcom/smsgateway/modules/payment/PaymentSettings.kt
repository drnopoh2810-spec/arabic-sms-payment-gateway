package me.capcom.smsgateway.modules.payment

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PaymentSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("payment_settings", Context.MODE_PRIVATE)
    
    var isEnabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) = prefs.edit { putBoolean("enabled", value) }
    
    var autoConfirmPayments: Boolean
        get() = prefs.getBoolean("auto_confirm", false)
        set(value) = prefs.edit { putBoolean("auto_confirm", value) }
    
    var showPaymentNotifications: Boolean
        get() = prefs.getBoolean("show_notifications", true)
        set(value) = prefs.edit { putBoolean("show_notifications", value) }
    
    var paymentWebhookUrl: String?
        get() = prefs.getString("webhook_url", null)
        set(value) = prefs.edit { putString("webhook_url", value) }
    
    var enabledWalletTypes: Set<PaymentWalletType>
        get() {
            val saved = prefs.getStringSet("enabled_wallets", null)
            return if (saved != null) {
                saved.mapNotNull { name ->
                    try {
                        PaymentWalletType.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }.toSet()
            } else {
                PaymentWalletType.values().toSet()
            }
        }
        set(value) = prefs.edit { 
            putStringSet("enabled_wallets", value.map { it.name }.toSet()) 
        }
    
    var minimumAmount: String
        get() = prefs.getString("minimum_amount", "0.01") ?: "0.01"
        set(value) = prefs.edit { putString("minimum_amount", value) }
    
    var maximumAmount: String?
        get() = prefs.getString("maximum_amount", null)
        set(value) = prefs.edit { putString("maximum_amount", value) }
    
    var retentionDays: Int
        get() = prefs.getInt("retention_days", 30)
        set(value) = prefs.edit { putInt("retention_days", value) }
    
    var requireConfirmation: Boolean
        get() = prefs.getBoolean("require_confirmation", true)
        set(value) = prefs.edit { putBoolean("require_confirmation", value) }
    
    var webhookTimeout: Int
        get() = prefs.getInt("webhook_timeout", 30)
        set(value) = prefs.edit { putInt("webhook_timeout", value) }
    
    var webhookRetries: Int
        get() = prefs.getInt("webhook_retries", 3)
        set(value) = prefs.edit { putInt("webhook_retries", value) }
}
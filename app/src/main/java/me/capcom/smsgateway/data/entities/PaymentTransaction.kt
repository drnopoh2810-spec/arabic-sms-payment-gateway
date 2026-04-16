package me.capcom.smsgateway.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.capcom.smsgateway.modules.payment.PaymentWalletType
import java.math.BigDecimal

@Entity(
    tableName = "payment_transactions",
    indices = [
        androidx.room.Index(value = ["createdAt"]),
        androidx.room.Index(value = ["walletType"]),
        androidx.room.Index(value = ["isConfirmed"]),
        androidx.room.Index(value = ["messageId"])
    ]
)
data class PaymentTransaction(
    @PrimaryKey val id: String,
    val messageId: String,
    val walletType: PaymentWalletType,
    val amount: String, // Store as string to preserve precision
    val currency: String = "EGP",
    val senderName: String?,
    val senderPhone: String?,
    val reference: String?,
    val transactionId: String?,
    val description: String?,
    val rawMessage: String,
    @ColumnInfo(defaultValue = "0")
    val isConfirmed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isProcessed: Boolean = false,
    val webhookUrl: String? = null,
    val webhookResponse: String? = null,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = null,
    val processedAt: Long? = null
) {
    fun getAmountAsBigDecimal(): BigDecimal {
        return try {
            BigDecimal(amount)
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }
}
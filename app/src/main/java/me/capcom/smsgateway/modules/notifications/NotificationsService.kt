package me.capcom.smsgateway.modules.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import me.capcom.smsgateway.MainActivity
import me.capcom.smsgateway.R

class NotificationsService(private val context: Context) {
    
    companion object {
        const val NOTIFICATION_ID_SMS_RECEIVED_WEBHOOK = 1001
        const val NOTIFICATION_ID_PAYMENT_DETECTED = 2001
        const val CHANNEL_ID_WEBHOOKS = "webhooks"
        const val CHANNEL_ID_PAYMENTS = "payments"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Webhooks channel
            val webhooksChannel = NotificationChannel(
                CHANNEL_ID_WEBHOOKS,
                context.getString(R.string.webhooks),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Webhook notifications"
            }
            
            // Payments channel
            val paymentsChannel = NotificationChannel(
                CHANNEL_ID_PAYMENTS,
                context.getString(R.string.payment_notifications),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Payment detection notifications"
            }
            
            notificationManager.createNotificationChannel(webhooksChannel)
            notificationManager.createNotificationChannel(paymentsChannel)
        }
    }
    
    fun notify(context: Context, notificationId: Int, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WEBHOOKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    fun showPaymentNotification(title: String, message: String, transactionId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("transaction_id", transactionId)
            putExtra("tab", "payments")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, transactionId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PAYMENTS)
            .setSmallIcon(R.drawable.ic_payment)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_PAYMENT_DETECTED + transactionId.hashCode(), notification)
    }
}
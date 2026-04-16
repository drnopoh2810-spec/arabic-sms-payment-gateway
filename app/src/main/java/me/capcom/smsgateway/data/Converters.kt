package me.capcom.smsgateway.data

import androidx.room.TypeConverter
import me.capcom.smsgateway.data.entities.MessageType
import me.capcom.smsgateway.domain.EntitySource
import me.capcom.smsgateway.domain.ProcessingState
import me.capcom.smsgateway.modules.incoming.db.IncomingMessageType
import me.capcom.smsgateway.modules.logs.db.LogEntry
import me.capcom.smsgateway.modules.payment.PaymentWalletType
import me.capcom.smsgateway.modules.webhooks.domain.WebHookEvent
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromProcessingState(value: ProcessingState): String {
        return value.name
    }

    @TypeConverter
    fun toProcessingState(value: String): ProcessingState {
        return ProcessingState.valueOf(value)
    }

    @TypeConverter
    fun fromEntitySource(value: EntitySource): String {
        return value.name
    }

    @TypeConverter
    fun toEntitySource(value: String): EntitySource {
        return EntitySource.valueOf(value)
    }

    @TypeConverter
    fun fromMessageType(value: MessageType): String {
        return value.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return MessageType.valueOf(value)
    }

    @TypeConverter
    fun fromWebHookEvent(value: WebHookEvent): String {
        return value.name
    }

    @TypeConverter
    fun toWebHookEvent(value: String): WebHookEvent {
        return WebHookEvent.valueOf(value)
    }

    @TypeConverter
    fun fromLogPriority(value: LogEntry.Priority): String {
        return value.name
    }

    @TypeConverter
    fun toLogPriority(value: String): LogEntry.Priority {
        return LogEntry.Priority.valueOf(value)
    }

    @TypeConverter
    fun fromIncomingMessageType(value: IncomingMessageType): String {
        return value.name
    }

    @TypeConverter
    fun toIncomingMessageType(value: String): IncomingMessageType {
        return IncomingMessageType.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentWalletType(value: PaymentWalletType): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentWalletType(value: String): PaymentWalletType {
        return PaymentWalletType.valueOf(value)
    }
}
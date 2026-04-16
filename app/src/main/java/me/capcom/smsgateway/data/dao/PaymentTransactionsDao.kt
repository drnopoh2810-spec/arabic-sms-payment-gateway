package me.capcom.smsgateway.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import me.capcom.smsgateway.data.entities.PaymentTransaction
import me.capcom.smsgateway.modules.payment.PaymentWalletType

@Dao
interface PaymentTransactionsDao {
    
    @Query("SELECT * FROM payment_transactions ORDER BY createdAt DESC LIMIT :limit")
    fun selectLast(limit: Int): LiveData<List<PaymentTransaction>>
    
    @Query("SELECT * FROM payment_transactions WHERE id = :id")
    fun get(id: String): PaymentTransaction?
    
    @Query("SELECT * FROM payment_transactions WHERE messageId = :messageId")
    fun getByMessageId(messageId: String): PaymentTransaction?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: PaymentTransaction)
    
    @Update
    fun update(transaction: PaymentTransaction)
    
    @Query("UPDATE payment_transactions SET isConfirmed = 1, confirmedAt = :confirmedAt WHERE id = :id")
    fun confirmTransaction(id: String, confirmedAt: Long)
    
    @Query("UPDATE payment_transactions SET isProcessed = 1, processedAt = :processedAt, webhookResponse = :response WHERE id = :id")
    fun markProcessed(id: String, processedAt: Long, response: String?)
    
    @Query("SELECT * FROM payment_transactions WHERE isConfirmed = 0 ORDER BY createdAt ASC")
    fun getPendingTransactions(): List<PaymentTransaction>
    
    @Query("SELECT * FROM payment_transactions WHERE isProcessed = 0 AND isConfirmed = 1 ORDER BY createdAt ASC")
    fun getUnprocessedConfirmedTransactions(): List<PaymentTransaction>
    
    @Query("SELECT COUNT(*) FROM payment_transactions WHERE walletType = :walletType AND createdAt >= :fromTime")
    fun countByWalletTypeFrom(walletType: PaymentWalletType, fromTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM payment_transactions WHERE isConfirmed = 1 AND createdAt >= :fromTime")
    fun countConfirmedFrom(fromTime: Long): Int
    
    @Query("SELECT SUM(CAST(amount AS REAL)) FROM payment_transactions WHERE isConfirmed = 1 AND createdAt >= :fromTime")
    fun sumConfirmedAmountFrom(fromTime: Long): Double?
    
    @Query("DELETE FROM payment_transactions WHERE createdAt < :beforeTime")
    fun deleteOlderThan(beforeTime: Long): Int
    
    @Query("SELECT * FROM payment_transactions WHERE walletType = :walletType ORDER BY createdAt DESC LIMIT :limit")
    fun getByWalletType(walletType: PaymentWalletType, limit: Int): List<PaymentTransaction>
    
    @Query("SELECT DISTINCT walletType FROM payment_transactions")
    fun getUsedWalletTypes(): List<PaymentWalletType>
}
package com.expensetracker.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE status = 'active' AND date >= :sinceMillis ORDER BY date DESC")
    fun getActiveSince(sinceMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'active' AND date >= :startMillis AND date < :endMillis ORDER BY date DESC")
    fun getActiveInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'active' AND classification = 'expense' AND date >= :sinceMillis")
    fun getTotalSpent(sinceMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'active' AND classification = 'expense' AND date >= :startMillis AND date < :endMillis")
    fun getTotalSpentInRange(startMillis: Long, endMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'active' AND classification IN ('income', 'refund') AND date >= :sinceMillis")
    fun getTotalIncome(sinceMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'active' AND classification IN ('income', 'refund') AND date >= :startMillis AND date < :endMillis")
    fun getTotalIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE status = 'pending_review' ORDER BY date DESC")
    fun getPendingReview(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'pending_review'")
    fun getPendingReviewCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE transactions SET status = 'hidden' WHERE status = 'pending_review'")
    suspend fun dismissAllPendingReview()
}

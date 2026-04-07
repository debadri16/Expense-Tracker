package com.expensetracker.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object Classification {
    const val EXPENSE = "expense"
    const val INCOME = "income"
    const val CC_PAYMENT = "cc_payment"
    const val CC_RECEIVED = "cc_received"
    const val REFUND = "refund"
    const val EMI = "emi"
    const val BILL_PAYMENT = "bill_payment"
}

object TxnStatus {
    const val ACTIVE = "active"
    const val PENDING_REVIEW = "pending_review"
    const val HIDDEN = "hidden"
}

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["dedupKey"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val classification: String,
    val bank: String,
    val cardLast4: String,
    val category: String,
    val merchant: String,
    val date: Long,
    val smsBody: String,
    val smsAddress: String,
    val templateId: String,
    val needsReview: Boolean,
    val dedupKey: String,
    val status: String = TxnStatus.ACTIVE
) {
    companion object {
        fun buildDedupKey(bank: String, cardLast4: String, amount: Double, dateMillis: Long, type: String): String {
            val roundedDate = dateMillis / 300_000 // 5-minute buckets
            return "${bank}_${cardLast4}_${amount}_${roundedDate}_${type}"
        }
    }
}

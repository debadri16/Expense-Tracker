package com.expensetracker.app.viewmodel

import android.app.Application
import android.app.NotificationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.db.AppDatabase
import com.expensetracker.app.data.db.Classification
import com.expensetracker.app.data.db.TransactionEntity
import com.expensetracker.app.data.db.TxnStatus
import com.expensetracker.app.data.sms.RawSms
import com.expensetracker.app.data.sms.SmsParser
import com.expensetracker.app.data.sms.SmsReader
import com.expensetracker.app.notification.TransactionNotificationHelper
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class TransactionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val txnDao = db.transactionDao()

    // --- Month selection (year, month where month is 0-based) ---

    private val _selectedMonth = MutableStateFlow(
        Calendar.getInstance().let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) }
    )
    val selectedMonth: StateFlow<Pair<Int, Int>> = _selectedMonth.asStateFlow()

    fun selectMonth(year: Int, month: Int) {
        _selectedMonth.value = year to month
    }

    private fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return start to end
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _selectedMonth
        .flatMapLatest { (year, month) ->
            val (start, end) = monthRange(year, month)
            txnDao.getActiveInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSpent: StateFlow<Double?> = _selectedMonth
        .flatMapLatest { (year, month) ->
            val (start, end) = monthRange(year, month)
            txnDao.getTotalSpentInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalIncome: StateFlow<Double?> = _selectedMonth
        .flatMapLatest { (year, month) ->
            val (start, end) = monthRange(year, month)
            txnDao.getTotalIncomeInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pendingReview: StateFlow<List<TransactionEntity>> = txnDao.getPendingReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReviewCount: StateFlow<Int> = txnDao.getPendingReviewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // --- Notification edit dialog state ---

    data class PendingEdit(val txn: TransactionEntity, val notifId: Int)

    private val _pendingEdit = MutableStateFlow<PendingEdit?>(null)
    val pendingEdit: StateFlow<PendingEdit?> = _pendingEdit.asStateFlow()

    fun setPendingEdit(txn: TransactionEntity, notifId: Int) {
        _pendingEdit.value = PendingEdit(txn, notifId)
    }

    fun addFromNotification(txn: TransactionEntity, notifId: Int) {
        cancelNotification(notifId)
        _pendingEdit.value = null
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.insert(txn.copy(status = TxnStatus.ACTIVE))
        }
    }

    fun billPaidFromNotification(txn: TransactionEntity, notifId: Int) {
        cancelNotification(notifId)
        _pendingEdit.value = null
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.insert(txn.copy(classification = Classification.BILL_PAYMENT, status = TxnStatus.ACTIVE))
        }
    }

    fun dismissFromNotification(txn: TransactionEntity, notifId: Int) {
        cancelNotification(notifId)
        _pendingEdit.value = null
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.insert(txn.copy(status = TxnStatus.HIDDEN))
        }
    }

    private fun cancelNotification(notifId: Int) {
        val manager = getApplication<Application>().getSystemService(NotificationManager::class.java)
        manager.cancel(notifId)
    }

    fun addManualTransaction(txn: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.insert(txn)
        }
    }

    // --- Existing transaction card edit dialog ---

    private val _selectedTxn = MutableStateFlow<TransactionEntity?>(null)
    val selectedTxn: StateFlow<TransactionEntity?> = _selectedTxn.asStateFlow()

    fun selectTransaction(txn: TransactionEntity) {
        _selectedTxn.value = txn
    }

    fun clearSelection() {
        _selectedTxn.value = null
    }

    fun updateTransaction(txn: TransactionEntity) {
        _selectedTxn.value = null
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.update(txn)
        }
    }

    fun deleteTransaction(txn: TransactionEntity) {
        _selectedTxn.value = null
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.updateStatus(txn.id, TxnStatus.HIDDEN)
        }
    }

    // --- Review screen actions ---

    fun approveReview(txn: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.update(txn.copy(status = TxnStatus.ACTIVE))
        }
    }

    fun billPaidReview(txn: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.update(txn.copy(status = TxnStatus.ACTIVE, classification = Classification.BILL_PAYMENT))
        }
    }

    fun dismissReview(txn: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.updateStatus(txn.id, TxnStatus.HIDDEN)
        }
    }

    fun dismissAllReview() {
        viewModelScope.launch(Dispatchers.IO) {
            txnDao.dismissAllPendingReview()
        }
    }

    // --- Sync SMS (inserts as pending_review) ---

    fun syncSmsTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _syncStatus.value = "Reading SMS..."

            try {
                val allSms = SmsReader.readAllSms(getApplication())
                _syncStatus.value = "Processing ${allSms.size} SMS..."

                val result = SmsParser.processBatch(allSms)
                val pending = result.transactions.map { it.copy(status = TxnStatus.PENDING_REVIEW) }
                val inserted = txnDao.insertAll(pending)
                val newCount = inserted.count { it != -1L }

                _syncStatus.value = "$newCount new for review (${result.matched} matched, ${result.skipped} skipped)"
            } catch (e: Exception) {
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Backup & Restore ---

    fun backupDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _syncStatus.value = "Backing up..."
                val context = getApplication<Application>()

                db.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).close()

                val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    dbFile.inputStream().use { input -> input.copyTo(output) }
                } ?: throw Exception("Could not open output file")

                _syncStatus.value = "Backup saved successfully"
            } catch (e: Exception) {
                _syncStatus.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun restoreDatabase(uri: Uri, onRestart: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _syncStatus.value = "Restoring..."
                val context = getApplication<Application>()

                val headerValid = context.contentResolver.openInputStream(uri)?.use { input ->
                    val header = ByteArray(16)
                    input.read(header) >= 16 &&
                        String(header, Charsets.ISO_8859_1).startsWith("SQLite format 3")
                } ?: false

                if (!headerValid) {
                    _syncStatus.value = "Invalid backup file"
                    return@launch
                }

                AppDatabase.closeAndClear()

                val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dbFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw Exception("Could not open backup file")

                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()

                withContext(Dispatchers.Main) { onRestart() }
            } catch (e: Exception) {
                _syncStatus.value = "Restore failed: ${e.message}"
            }
        }
    }

    // --- Debug test ---

    fun sendTestNotification(sender: String, body: String) {
        val fakeSms = RawSms(
            address = sender,
            body = body,
            dateMillis = System.currentTimeMillis()
        )
        val txn = SmsParser.parse(fakeSms)
        if (txn != null) {
            TransactionNotificationHelper.showTransactionNotification(getApplication(), txn)
        } else {
            _syncStatus.value = "No transaction detected in that SMS"
        }
    }
}

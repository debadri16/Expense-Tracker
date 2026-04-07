package com.expensetracker.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.expensetracker.app.data.db.AppDatabase
import com.expensetracker.app.data.db.Classification
import com.expensetracker.app.data.db.TxnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ADD = "com.expensetracker.ACTION_ADD_TRANSACTION"
        const val ACTION_BILL_PAID = "com.expensetracker.ACTION_BILL_PAID"
        const val ACTION_DISMISS = "com.expensetracker.ACTION_DISMISS_TRANSACTION"
        const val EXTRA_NOTIF_ID = "notif_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notifId)

        when (intent.action) {
            ACTION_ADD -> {
                val txn = TransactionNotificationHelper.bundleToTxn(intent) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getInstance(context).transactionDao().insert(txn.copy(status = TxnStatus.ACTIVE))
                }
            }
            ACTION_BILL_PAID -> {
                val txn = TransactionNotificationHelper.bundleToTxn(intent) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getInstance(context).transactionDao()
                        .insert(txn.copy(classification = Classification.BILL_PAYMENT, status = TxnStatus.ACTIVE))
                }
            }
            ACTION_DISMISS -> {
                val txn = TransactionNotificationHelper.bundleToTxn(intent) ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getInstance(context).transactionDao().insert(txn.copy(status = TxnStatus.HIDDEN))
                }
            }
        }
    }
}

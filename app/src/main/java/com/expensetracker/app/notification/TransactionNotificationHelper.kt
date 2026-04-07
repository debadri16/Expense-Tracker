package com.expensetracker.app.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.expensetracker.app.MainActivity
import com.expensetracker.app.R
import com.expensetracker.app.data.db.TransactionEntity
import java.text.NumberFormat
import java.util.Locale

object TransactionNotificationHelper {

    const val CHANNEL_ID = "transaction_alerts"
    const val ACTION_EDIT_TRANSACTION = "com.expensetracker.ACTION_EDIT_TRANSACTION"
    const val EXTRA_NOTIF_ID = "notif_id"
    private const val CHANNEL_NAME = "Transaction Alerts"
    private const val CHANNEL_DESC = "Notifications for detected bank transactions"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showTransactionNotification(context: Context, txn: TransactionEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notifId = txn.dedupKey.hashCode()
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val amountStr = currencyFormat.format(txn.amount)

        val typeLabel = if (txn.type == "debit") "Debited" else "Credited"
        val title = "$typeLabel $amountStr"

        val parts = mutableListOf<String>()
        if (txn.merchant.isNotEmpty()) parts.add(txn.merchant)
        if (txn.bank.isNotEmpty()) parts.add(txn.bank)
        if (txn.cardLast4.isNotEmpty()) parts.add("••${txn.cardLast4}")
        val subtitle = parts.joinToString(" • ")

        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_EDIT_TRANSACTION
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtras(txnToBundle(txn))
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPending = PendingIntent.getActivity(
            context, notifId + 3, editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val addIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtras(txnToBundle(txn))
        }
        val addPending = PendingIntent.getBroadcast(
            context, notifId, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val billPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_BILL_PAID
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtras(txnToBundle(txn))
        }
        val billPaidPending = PendingIntent.getBroadcast(
            context, notifId + 1, billPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtras(txnToBundle(txn))
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, notifId + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$subtitle\n${txn.category.replaceFirstChar { it.uppercase() }} • ${txn.classification.replaceFirstChar { it.uppercase() }}")
            )
            .setContentIntent(editPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_add, "Add", addPending)
            .addAction(R.drawable.ic_add, "Bill Paid", billPaidPending)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPending)
            .build()

        notification.flags = notification.flags or
                Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_NO_CLEAR

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun txnToBundle(txn: TransactionEntity): android.os.Bundle {
        return android.os.Bundle().apply {
            putDouble("amount", txn.amount)
            putString("type", txn.type)
            putString("classification", txn.classification)
            putString("bank", txn.bank)
            putString("cardLast4", txn.cardLast4)
            putString("category", txn.category)
            putString("merchant", txn.merchant)
            putLong("date", txn.date)
            putString("smsBody", txn.smsBody)
            putString("smsAddress", txn.smsAddress)
            putString("templateId", txn.templateId)
            putString("dedupKey", txn.dedupKey)
        }
    }

    fun bundleToTxn(intent: Intent): TransactionEntity? {
        val amount = intent.getDoubleExtra("amount", -1.0)
        if (amount < 0) return null
        return TransactionEntity(
            amount = amount,
            type = intent.getStringExtra("type") ?: "debit",
            classification = intent.getStringExtra("classification") ?: "expense",
            bank = intent.getStringExtra("bank") ?: "",
            cardLast4 = intent.getStringExtra("cardLast4") ?: "",
            category = intent.getStringExtra("category") ?: "other",
            merchant = intent.getStringExtra("merchant") ?: "",
            date = intent.getLongExtra("date", System.currentTimeMillis()),
            smsBody = intent.getStringExtra("smsBody") ?: "",
            smsAddress = intent.getStringExtra("smsAddress") ?: "",
            templateId = intent.getStringExtra("templateId") ?: "",
            needsReview = false,
            dedupKey = intent.getStringExtra("dedupKey") ?: ""
        )
    }
}

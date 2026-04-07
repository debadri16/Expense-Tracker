package com.expensetracker.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expensetracker.app.notification.TransactionNotificationHelper

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (msg in messages) {
            val sms = RawSms(
                address = msg.originatingAddress ?: "",
                body = msg.messageBody ?: "",
                dateMillis = msg.timestampMillis
            )
            val txn = SmsParser.parse(sms) ?: continue
            TransactionNotificationHelper.showTransactionNotification(context, txn)
        }
    }
}

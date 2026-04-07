package com.expensetracker.app

import android.app.Application
import com.expensetracker.app.notification.TransactionNotificationHelper

class ExpenseTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TransactionNotificationHelper.createChannel(this)
    }
}

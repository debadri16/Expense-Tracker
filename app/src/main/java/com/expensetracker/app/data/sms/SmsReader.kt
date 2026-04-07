package com.expensetracker.app.data.sms

import android.content.Context
import android.net.Uri
import java.util.Calendar

object SmsReader {

    private val SMS_URI: Uri = Uri.parse("content://sms/inbox")

    private fun oneMonthAgoMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.timeInMillis
    }

    fun readAllSms(context: Context, sinceMillis: Long = oneMonthAgoMillis()): List<RawSms> {
        val smsList = mutableListOf<RawSms>()
        val cursor = context.contentResolver.query(
            SMS_URI,
            arrayOf("address", "body", "date"),
            "date >= ?",
            arrayOf(sinceMillis.toString()),
            "date DESC"
        ) ?: return smsList

        cursor.use {
            val addressIdx = it.getColumnIndexOrThrow("address")
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val dateIdx = it.getColumnIndexOrThrow("date")

            while (it.moveToNext()) {
                smsList.add(
                    RawSms(
                        address = it.getString(addressIdx) ?: "",
                        body = it.getString(bodyIdx) ?: "",
                        dateMillis = it.getLong(dateIdx)
                    )
                )
            }
        }
        return smsList
    }
}

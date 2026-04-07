package com.expensetracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.expensetracker.app.notification.TransactionNotificationHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.expensetracker.app.ui.screens.AnalysisScreen
import com.expensetracker.app.ui.screens.ReviewScreen
import com.expensetracker.app.ui.screens.TransactionListScreen
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import com.expensetracker.app.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private var hasPermission by mutableStateOf(false)
    private var showReview by mutableStateOf(false)
    private var showAnalysis by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasPermission = checkSmsPermission()
        handleTransactionIntent(intent)

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!hasPermission) {
                        PermissionScreen(onRequestPermission = { requestSmsPermission() })
                    } else if (showReview) {
                        ReviewScreen(viewModel, onBack = { showReview = false })
                    } else if (showAnalysis) {
                        AnalysisScreen(viewModel, onBack = { showAnalysis = false })
                    } else {
                        TransactionListScreen(
                            viewModel,
                            onOpenReview = { showReview = true },
                            onOpenAnalysis = { showAnalysis = true }
                        )
                    }
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleTransactionIntent(intent)
    }

    private fun handleTransactionIntent(intent: Intent?) {
        if (intent?.action != TransactionNotificationHelper.ACTION_EDIT_TRANSACTION) return
        val txn = TransactionNotificationHelper.bundleToTxn(intent) ?: return
        val notifId = intent.getIntExtra(TransactionNotificationHelper.EXTRA_NOTIF_ID, 0)
        viewModel.setPendingEdit(txn, notifId)
        intent.action = null
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@androidx.compose.runtime.Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SMS Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "This app reads your bank SMS messages to automatically track expenses. No data leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

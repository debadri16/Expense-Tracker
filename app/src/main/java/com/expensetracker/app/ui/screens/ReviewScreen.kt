package com.expensetracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expensetracker.app.data.db.TransactionEntity
import com.expensetracker.app.ui.theme.CreditGreen
import com.expensetracker.app.ui.theme.DebitRed
import com.expensetracker.app.viewmodel.TransactionViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: TransactionViewModel, onBack: () -> Unit) {
    val pending by viewModel.pendingReview.collectAsState()
    var editingTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    editingTxn?.let { txn ->
        ReviewEditDialog(
            txn = txn,
            onAdd = { edited ->
                viewModel.approveReview(edited)
                editingTxn = null
            },
            onBillPaid = { edited ->
                viewModel.billPaidReview(edited)
                editingTxn = null
            },
            onDismiss = {
                viewModel.dismissReview(txn)
                editingTxn = null
            },
            onCancel = { editingTxn = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Transactions (${pending.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pending.isNotEmpty()) {
                        TextButton(onClick = { viewModel.dismissAllReview() }) {
                            Text("Dismiss All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (pending.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No transactions to review",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tap Sync SMS on the main screen to scan for transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(pending, key = { it.id }) { txn ->
                    ReviewCard(txn, onClick = { editingTxn = txn })
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ReviewCard(txn: TransactionEntity, onClick: () -> Unit) {
    val amountColor = if (txn.type == "credit") CreditGreen else DebitRed
    val prefix = if (txn.type == "credit") "+ " else "- "

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.merchant.ifEmpty { txn.category.replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            append(txn.bank)
                            if (txn.cardLast4.isNotEmpty()) append(" •• ${txn.cardLast4}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(txn.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${formatCurrency(txn.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = txn.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewEditDialog(
    txn: TransactionEntity,
    onAdd: (TransactionEntity) -> Unit,
    onBillPaid: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf(txn.amount.toString()) }
    var merchant by remember { mutableStateOf(txn.merchant) }
    var bank by remember { mutableStateOf(txn.bank) }
    var cardLast4 by remember { mutableStateOf(txn.cardLast4) }
    var category by remember { mutableStateOf(txn.category) }
    var type by remember { mutableStateOf(txn.type) }

    fun buildEdited(): TransactionEntity {
        val parsedAmount = amount.toDoubleOrNull() ?: txn.amount
        return txn.copy(
            amount = parsedAmount,
            merchant = merchant.trim(),
            bank = bank.trim(),
            cardLast4 = cardLast4.trim(),
            category = category.trim(),
            type = type.trim()
        )
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (txn.type == "debit") "Review Debit" else "Review Credit",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (txn.smsBody.isNotEmpty()) {
                    Text(
                        txn.smsBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                TransactionFormFields(
                    amount, { amount = it }, type, { type = it },
                    merchant, { merchant = it }, bank, { bank = it },
                    cardLast4, { cardLast4 = it }, category, { category = it }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAdd(buildEdited()) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add") }
                    OutlinedButton(
                        onClick = { onBillPaid(buildEdited()) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Bill Paid") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Dismiss") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

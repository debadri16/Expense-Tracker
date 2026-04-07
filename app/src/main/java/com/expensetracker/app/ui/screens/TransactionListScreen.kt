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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.BarChart
import com.expensetracker.app.BuildConfig
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expensetracker.app.data.db.Classification
import com.expensetracker.app.data.db.TransactionEntity
import com.expensetracker.app.data.db.TxnStatus
import com.expensetracker.app.ui.theme.CreditGreen
import com.expensetracker.app.ui.theme.DebitRed
import com.expensetracker.app.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onOpenReview: () -> Unit,
    onOpenAnalysis: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val pendingEdit by viewModel.pendingEdit.collectAsState()
    val selectedTxn by viewModel.selectedTxn.collectAsState()
    val reviewCount by viewModel.pendingReviewCount.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var showTestDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddTransactionDialog(
            onAdd = { txn ->
                viewModel.addManualTransaction(txn)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showTestDialog) {
        TestSmsDialog(
            onSend = { sender, body ->
                viewModel.sendTestNotification(sender, body)
                showTestDialog = false
            },
            onDismiss = { showTestDialog = false }
        )
    }

    pendingEdit?.let { edit ->
        EditTransactionDialog(
            txn = edit.txn,
            onAdd = { edited -> viewModel.addFromNotification(edited, edit.notifId) },
            onBillPaid = { edited -> viewModel.billPaidFromNotification(edited, edit.notifId) },
            onDismiss = { viewModel.dismissFromNotification(edit.txn, edit.notifId) }
        )
    }

    selectedTxn?.let { txn ->
        ExistingTransactionDialog(
            txn = txn,
            onSave = { edited -> viewModel.updateTransaction(edited) },
            onDelete = { viewModel.deleteTransaction(txn) },
            onDismiss = { viewModel.clearSelection() }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        if (BuildConfig.DEBUG) {
                            IconButton(onClick = { showTestDialog = true }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Test Notification")
                            }
                        }
                        if (reviewCount > 0) {
                            IconButton(onClick = onOpenReview) {
                                Icon(Icons.Default.RateReview, contentDescription = "Review")
                            }
                        }
                        IconButton(onClick = onOpenAnalysis) {
                            Icon(Icons.Default.BarChart, contentDescription = "Analysis")
                        }
                        IconButton(onClick = { viewModel.syncSmsTransactions() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync SMS")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SummaryCard(
                totalSpent = totalSpent ?: 0.0,
                totalIncome = totalIncome ?: 0.0,
                expenseCount = transactions.count { it.classification == Classification.EXPENSE },
                selectedYear = selectedMonth.first,
                selectedMonthIndex = selectedMonth.second,
                onMonthSelected = { year, month -> viewModel.selectMonth(year, month) }
            )

            if (syncStatus.isNotEmpty()) {
                Text(
                    text = syncStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (reviewCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable(onClick = onOpenReview),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "$reviewCount transaction${if (reviewCount != 1) "s" else ""} pending review",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            if (transactions.isEmpty() && !isLoading) {
                val now = Calendar.getInstance()
                val isCurrentMonth = selectedMonth.first == now.get(Calendar.YEAR) &&
                    selectedMonth.second == now.get(Calendar.MONTH)
                val monthLabel = formatMonthYear(selectedMonth.first, selectedMonth.second)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isCurrentMonth) "No transactions yet"
                        else "No transactions in $monthLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isCurrentMonth) "Tap the sync button to scan your SMS for transactions"
                        else "Try selecting a different month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(transactions, key = { it.id }) { txn ->
                        TransactionCard(txn, onClick = { viewModel.selectTransaction(txn) })
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryCard(
    totalSpent: Double,
    totalIncome: Double,
    expenseCount: Int,
    selectedYear: Int,
    selectedMonthIndex: Int,
    onMonthSelected: (year: Int, month: Int) -> Unit
) {
    var monthExpanded by remember { mutableStateOf(false) }
    val now = remember { Calendar.getInstance() }
    val currentYear = remember { now.get(Calendar.YEAR) }
    val currentMonth = remember { now.get(Calendar.MONTH) }

    val monthOptions = remember {
        (0..11).map { offset ->
            Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1)
                add(Calendar.MONTH, -offset)
            }.let { cal -> cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH) }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuBox(
                expanded = monthExpanded,
                onExpandedChange = { monthExpanded = it }
            ) {
                Row(
                    modifier = Modifier
                        .menuAnchor()
                        .clickable { monthExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCurrentMonth = selectedYear == currentYear && selectedMonthIndex == currentMonth
                    Text(
                        if (isCurrentMonth) "This Month" else formatMonthYear(selectedYear, selectedMonthIndex),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded)
                }
                ExposedDropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    monthOptions.forEachIndexed { index, (year, month) ->
                        DropdownMenuItem(
                            text = {
                                Text(if (index == 0) "This Month" else formatMonthYear(year, month))
                            },
                            onClick = {
                                onMonthSelected(year, month)
                                monthExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        formatCurrency(totalSpent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DebitRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        formatCurrency(totalIncome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CreditGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$expenseCount expenses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TransactionCard(txn: TransactionEntity, onClick: () -> Unit) {
    val isExcluded = txn.classification in listOf(
        Classification.CC_PAYMENT, Classification.CC_RECEIVED, Classification.BILL_PAYMENT
    )
    val cardAlpha = if (isExcluded) 0.5f else 1f

    val amountColor = when (txn.classification) {
        Classification.REFUND, Classification.INCOME, Classification.CC_RECEIVED -> CreditGreen
        Classification.CC_PAYMENT, Classification.BILL_PAYMENT -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> DebitRed
    }

    val prefix = when (txn.classification) {
        Classification.REFUND, Classification.INCOME, Classification.CC_RECEIVED -> "+ "
        else -> "- "
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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

internal val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
internal val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

internal fun formatCurrency(amount: Double): String = currencyFormat.format(amount)
internal fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

internal fun formatMonthYear(year: Int, month: Int): String {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionFormFields(
    amount: String,
    onAmountChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    bank: String,
    onBankChange: (String) -> Unit,
    cardLast4: String,
    onCardChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit
) {
    val typeOptions = listOf("debit", "credit")
    var typeExpanded by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onAmountChange(newValue)
                }
            },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = type.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Type") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                typeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onTypeChange(option)
                            typeExpanded = false
                        }
                    )
                }
            }
        }
    }
    OutlinedTextField(
        value = merchant,
        onValueChange = onMerchantChange,
        label = { Text("Merchant") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = bank,
            onValueChange = onBankChange,
            label = { Text("Bank") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = cardLast4,
            onValueChange = onCardChange,
            label = { Text("Card ••") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = category,
        onValueChange = onCategoryChange,
        label = { Text("Category") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EditTransactionDialog(
    txn: TransactionEntity,
    onAdd: (TransactionEntity) -> Unit,
    onBillPaid: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
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
            type = type.trim(),
            dedupKey = TransactionEntity.buildDedupKey(
                bank.trim(), cardLast4.trim(), parsedAmount, txn.date, type.trim()
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (txn.type == "debit") "Debit Detected" else "Credit Detected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    txn.smsBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun ExistingTransactionDialog(
    txn: TransactionEntity,
    onSave: (TransactionEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
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
        onDismissRequest = onDismiss,
        title = {
            Text(
                txn.merchant.ifEmpty { txn.category.replaceFirstChar { it.uppercase() } },
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
                        onClick = { onSave(buildEdited()) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun AddTransactionDialog(
    onAdd: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("debit") }
    var merchant by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("other") }

    val parsedAmount = amount.toDoubleOrNull()
    val isValid = parsedAmount != null && parsedAmount > 0 &&
        type.isNotBlank() && bank.isNotBlank() && cardLast4.isNotBlank()

    fun buildTxn(): TransactionEntity {
        val amt = parsedAmount ?: 0.0
        val now = System.currentTimeMillis()
        val classification = if (type.trim().lowercase() == "credit")
            Classification.INCOME else Classification.EXPENSE
        return TransactionEntity(
            amount = amt,
            type = type.trim(),
            classification = classification,
            bank = bank.trim(),
            cardLast4 = cardLast4.trim(),
            category = category.trim().ifEmpty { "other" },
            merchant = merchant.trim(),
            date = now,
            smsBody = "",
            smsAddress = "",
            templateId = "",
            needsReview = false,
            dedupKey = TransactionEntity.buildDedupKey(
                bank.trim(), cardLast4.trim(), amt, now, type.trim()
            ),
            status = TxnStatus.ACTIVE
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Transaction", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransactionFormFields(
                    amount, { amount = it }, type, { type = it },
                    merchant, { merchant = it }, bank, { bank = it },
                    cardLast4, { cardLast4 = it }, category, { category = it }
                )
                if (!isValid && (amount.isNotEmpty() || bank.isNotEmpty() || cardLast4.isNotEmpty())) {
                    Text(
                        "Amount, Type, Bank, and Card are required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAdd(buildTxn()) },
                        modifier = Modifier.weight(1f),
                        enabled = isValid
                    ) { Text("Add") }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun TestSmsDialog(
    onSend: (sender: String, body: String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultSender = "VM-HDFCBK"
    val defaultBody = "Rs 530.00 debited from a/c **1234 on 30-Mar-26 on Amazon Pay. Avl bal Rs 12,345.67"

    var sender by remember { mutableStateOf(defaultSender) }
    var body by remember { mutableStateOf(defaultBody) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test SMS Notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender (e.g. VM-HDFCBK)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("SMS Body") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(sender, body) },
                enabled = sender.isNotBlank() && body.isNotBlank()
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

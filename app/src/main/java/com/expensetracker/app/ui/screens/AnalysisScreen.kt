package com.expensetracker.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetracker.app.data.db.Classification
import com.expensetracker.app.ui.theme.BankAmber
import com.expensetracker.app.ui.theme.BankBlue
import com.expensetracker.app.ui.theme.BankCyan
import com.expensetracker.app.ui.theme.BankDeepOrange
import com.expensetracker.app.ui.theme.BankDeepPurple
import com.expensetracker.app.ui.theme.BankGreen
import com.expensetracker.app.ui.theme.BankLime
import com.expensetracker.app.ui.theme.BankPurple
import com.expensetracker.app.ui.theme.BankRed
import com.expensetracker.app.ui.theme.BankTealDark
import com.expensetracker.app.ui.theme.ChartAmber
import com.expensetracker.app.ui.theme.ChartBlue
import com.expensetracker.app.ui.theme.ChartBlueGrey
import com.expensetracker.app.ui.theme.ChartBrown
import com.expensetracker.app.ui.theme.ChartGreen
import com.expensetracker.app.ui.theme.ChartIndigo
import com.expensetracker.app.ui.theme.ChartOrange
import com.expensetracker.app.ui.theme.ChartPink
import com.expensetracker.app.ui.theme.ChartPurple
import com.expensetracker.app.ui.theme.ChartRed
import com.expensetracker.app.ui.theme.ChartTeal
import com.expensetracker.app.ui.theme.CreditGreen
import com.expensetracker.app.ui.theme.DebitRed
import com.expensetracker.app.viewmodel.TransactionViewModel
import java.util.Calendar
import kotlin.math.roundToInt

private val CategoryColorMap = mapOf(
    "food" to ChartOrange,
    "grocery" to ChartGreen,
    "shopping" to ChartBlue,
    "travel" to ChartPurple,
    "fuel" to ChartAmber,
    "entertainment" to ChartPink,
    "health" to ChartRed,
    "education" to ChartIndigo,
    "utilities" to ChartTeal,
    "emi" to ChartBrown,
    "other" to ChartBlueGrey,
)

private val BankColorPalette = listOf(
    BankBlue, BankGreen, BankRed, BankPurple, BankDeepOrange,
    BankCyan, BankAmber, BankDeepPurple, BankTealDark, BankLime,
)

private fun getCategoryColor(category: String): Color =
    CategoryColorMap[category.lowercase()] ?: ChartBlueGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: TransactionViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val expenses = remember(transactions) {
        transactions.filter { it.classification == Classification.EXPENSE }
    }
    val incomeTransactions = remember(transactions) {
        transactions.filter {
            it.classification in listOf(Classification.INCOME, Classification.REFUND)
        }
    }
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category.lowercase() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    val bankCardTotals = remember(expenses) {
        expenses.groupBy { "${it.bank}|${it.cardLast4}" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    val bankColorMap = remember(bankCardTotals) {
        bankCardTotals.map { it.first.substringBefore("|") }
            .distinct()
            .mapIndexed { i, bank -> bank to BankColorPalette[i % BankColorPalette.size] }
            .toMap()
    }
    val totalIncome = remember(incomeTransactions) { incomeTransactions.sumOf { it.amount } }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            MonthSelectorCard(
                selectedYear = selectedMonth.first,
                selectedMonthIndex = selectedMonth.second,
                onMonthSelected = { y, m -> viewModel.selectMonth(y, m) }
            )

            if (expenses.isEmpty() && incomeTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No data for this month",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Select a month with transactions to see analysis",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                IncomeExpenseSection(totalIncome, totalExpense, categoryTotals)

                if (categoryTotals.isNotEmpty()) {
                    CategoryPieChartSection(categoryTotals, totalExpense)
                }

                if (bankCardTotals.isNotEmpty()) {
                    BankCardBarSection(bankCardTotals, bankColorMap)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthSelectorCard(
    selectedYear: Int,
    selectedMonthIndex: Int,
    onMonthSelected: (year: Int, month: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val now = remember { Calendar.getInstance() }
    val currentYear = remember { now.get(Calendar.YEAR) }
    val currentMonth = remember { now.get(Calendar.MONTH) }
    val monthOptions = remember {
        (0..11).map { offset ->
            Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1)
                add(Calendar.MONTH, -offset)
            }.let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                Row(
                    modifier = Modifier
                        .menuAnchor()
                        .clickable { expanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCurrentMonth =
                        selectedYear == currentYear && selectedMonthIndex == currentMonth
                    Text(
                        if (isCurrentMonth) "This Month"
                        else formatMonthYear(selectedYear, selectedMonthIndex),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    monthOptions.forEachIndexed { index, (year, month) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (index == 0) "This Month"
                                    else formatMonthYear(year, month)
                                )
                            },
                            onClick = {
                                onMonthSelected(year, month)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseSection(
    totalIncome: Double,
    totalExpense: Double,
    categoryTotals: List<Pair<String, Double>>
) {
    val maxAmount = maxOf(totalIncome, totalExpense)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Income vs Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Income",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatCurrency(totalIncome),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CreditGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (maxAmount > 0 && totalIncome > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (totalIncome / maxAmount).toFloat())
                            .clip(RoundedCornerShape(8.dp))
                            .background(CreditGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Expenses",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatCurrency(totalExpense),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DebitRed
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (maxAmount > 0 && totalExpense > 0) {
                    val filtered = categoryTotals.filter { it.second > 0 }
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (totalExpense / maxAmount).toFloat())
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        filtered.forEach { (category, amount) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(amount.toFloat())
                                    .background(getCategoryColor(category))
                            )
                        }
                    }
                }
            }

            if (categoryTotals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                CategoryLegendGrid(categoryTotals, totalExpense)
            }
        }
    }
}

@Composable
private fun CategoryPieChartSection(
    categoryTotals: List<Pair<String, Double>>,
    totalExpense: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Expenses by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center) {
                val surfaceColor = MaterialTheme.colorScheme.surface

                Canvas(modifier = Modifier.size(220.dp)) {
                    val total = categoryTotals.sumOf { it.second }.toFloat()
                    if (total == 0f) return@Canvas

                    val diameter = minOf(size.width, size.height)
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    categoryTotals.forEach { (category, amount) ->
                        val sweepAngle = (amount.toFloat() / total) * 360f
                        drawArc(
                            color = getCategoryColor(category),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = topLeft,
                            size = arcSize
                        )
                        startAngle += sweepAngle
                    }

                    drawCircle(
                        color = surfaceColor,
                        radius = diameter * 0.28f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrency(totalExpense),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            CategoryLegendGrid(categoryTotals, totalExpense)
        }
    }
}

@Composable
private fun BankCardBarSection(
    bankCardTotals: List<Pair<String, Double>>,
    bankColorMap: Map<String, Color>
) {
    val maxAmount = bankCardTotals.maxOfOrNull { it.second } ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Expenses by Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            bankCardTotals.forEach { (key, amount) ->
                val bank = key.substringBefore("|")
                val card = key.substringAfter("|")
                val label = if (card.isNotEmpty()) "$bank •• $card" else bank
                val color = bankColorMap[bank] ?: BankColorPalette[0]
                val fraction = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formatCurrency(amount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val banks = bankColorMap.toList()
            if (banks.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Bank Colors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    banks.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { (bank, color) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        bank,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendGrid(
    categoryTotals: List<Pair<String, Double>>,
    totalExpense: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categoryTotals.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (category, amount) ->
                    val percent = if (totalExpense > 0)
                        (amount / totalExpense * 100).roundToInt() else 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(category))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${category.replaceFirstChar { it.uppercase() }} $percent%",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

package com.motocare.app.ui.loan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.ui.components.MotoCareEmptyState
import com.motocare.app.ui.components.MotoCareLoadingState
import com.motocare.app.ui.components.MotoCareNoMotorcycleState
import com.motocare.app.ui.components.MotoCareSummaryCard
import com.motocare.app.ui.components.MotoCareDateField
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.asPeso
import com.motocare.app.util.isBlankOrValidMoney
import com.motocare.app.util.toCentavosOrNull
import java.time.LocalDate

private val paymentStatuses = listOf("PAID_ON_TIME", "PAID_LATE", "MISSED", "PENDING")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    onBack: () -> Unit,
    onManageMotorcycles: () -> Unit,
    viewModel: LoanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSetup by remember { mutableStateOf(false) }
    var paymentToMark by remember { mutableStateOf<LoanPaymentEntity?>(null) }
    var deleteLoan by remember { mutableStateOf<LoanEntity?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Financing") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        if (state.isLoading) {
            MotoCareLoadingState(Modifier.padding(padding))
        } else if (state.motorcycle == null) {
            MotoCareNoMotorcycleState(onManageMotorcycles, Modifier.padding(padding))
        } else if (state.loan == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
                MotoCareEmptyState(
                    title = "No financing plan",
                    detail = "Add the agreement details to track due dates, rebates, and payment progress.",
                    icon = Icons.Outlined.Payments,
                    actionLabel = "Set up financing",
                    onAction = { showSetup = true },
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(state.motorcycle?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    state.summary?.let { summary ->
                        MotoCareSummaryCard(
                            label = "Financing progress",
                            value = "${summary.remainingPayments} payments remaining",
                            detail = "Total paid ${summary.totalPaidCentavos.asPeso()} • rebates ${summary.rebatesEarnedCentavos.asPeso()}" +
                                (summary.nextPaymentDate?.let { "\nNext due ${it.asDisplayDate()} • ${summary.daysUntilDue} days" } ?: "") +
                                (summary.estimatedPayoffDate?.let { "\nEstimated payoff ${it.asDisplayDate()}" } ?: ""),
                            icon = Icons.Outlined.Payments,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = { showSetup = true }) { Text("Edit financing") }
                        TextButton(onClick = { state.loan?.let { deleteLoan = it } }) { Text("Remove financing") }
                    }
                    Text("Payment schedule", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                }
                items(state.payments, key = { it.id }) { payment ->
                    Card(Modifier.fillMaxWidth().clickable { paymentToMark = payment }) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Payment ${payment.installmentNumber}", fontWeight = FontWeight.SemiBold)
                                Text(LocalDate.ofEpochDay(payment.dueEpochDay).asDisplayDate())
                            }
                            Column {
                                Text(payment.status.replace('_', ' '), color = MaterialTheme.colorScheme.primary)
                                if (payment.amountCentavos > 0) Text(payment.amountCentavos.asPeso())
                            }
                        }
                    }
                }
            }
        }
    }
    if (showSetup) {
        LoanSetupDialog(
            existing = state.loan,
            onDismiss = { showSetup = false },
            onSave = { input -> viewModel.configure(input, state.loan) { showSetup = false } },
        )
    }
    paymentToMark?.let { payment ->
        var paidDate by remember(payment.id) {
            mutableStateOf(payment.paidEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now())
        }
        AlertDialog(
            onDismissRequest = { paymentToMark = null },
            title = { Text("Payment ${payment.installmentNumber}") },
            text = {
                Column {
                    MotoCareDateField(paidDate, { paidDate = it }, label = "Payment date")
                    Text(
                        "The selected date is used when marking this payment paid.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    paymentStatuses.forEach { status ->
                        TextButton(onClick = { viewModel.mark(payment, status, paidDate); paymentToMark = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(status.replace('_', ' '))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { paymentToMark = null }) { Text("Cancel") } },
        )
    }
    deleteLoan?.let { loan ->
        AlertDialog(
            onDismissRequest = { deleteLoan = null },
            title = { Text("Remove financing plan?") },
            text = { Text("This removes the financing schedule and payment statuses from MotoCare. Other motorcycle records are not affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(loan); deleteLoan = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteLoan = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LoanSetupDialog(existing: LoanEntity? = null, onDismiss: () -> Unit, onSave: (LoanInput) -> Unit) {
    var cash by remember(existing) { mutableStateOf(existing?.cashPriceCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var down by remember(existing) { mutableStateOf(existing?.downPaymentCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var monthly by remember(existing) { mutableStateOf(existing?.monthlyPaymentCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var term by remember(existing) { mutableStateOf(existing?.termMonths?.toString() ?: "12") }
    var dueDay by remember(existing) { mutableStateOf(existing?.paymentDueDay?.toString().orEmpty()) }
    var rebate by remember(existing) { mutableStateOf(existing?.rebateCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var start by remember(existing) { mutableStateOf(existing?.startEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var confirmReplacement by remember { mutableStateOf(false) }
    val input = LoanInput(cash, down, monthly, term, dueDay, rebate, start.toString(), notes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Set up financing" else "Edit financing") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField(cash, { cash = it }, "Cash price (optional)")
                MoneyField(down, { down = it }, "Down payment")
                MoneyField(monthly, { monthly = it }, "Monthly payment")
                DigitsField(term, { term = it }, "Term (months)")
                DigitsField(dueDay, { dueDay = it }, "Payment due day")
                MoneyField(rebate, { rebate = it }, "On-time rebate")
                MotoCareDateField(start, { start = it }, label = "Financing start")
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = monthly.toCentavosOrNull()?.let { it > 0 } == true &&
                    cash.isBlankOrValidMoney() &&
                    down.isBlankOrValidMoney() &&
                    rebate.isBlankOrValidMoney() &&
                    term.toIntOrNull()?.let { it > 0 } == true &&
                    (dueDay.isBlank() || dueDay.toIntOrNull() in 1..31),
                onClick = { if (existing == null) onSave(input) else confirmReplacement = true },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (confirmReplacement) {
        AlertDialog(
            onDismissRequest = { confirmReplacement = false },
            title = { Text("Rebuild payment schedule?") },
            text = { Text("Saving financing changes rebuilds the installment schedule and clears its existing payment statuses.") },
            confirmButton = {
                TextButton(onClick = { confirmReplacement = false; onSave(input) }) { Text("Rebuild and save") }
            },
            dismissButton = { TextButton(onClick = { confirmReplacement = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MoneyField(value: String, onChange: (String) -> Unit, label: String) =
    OutlinedTextField(value, { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())

@Composable
private fun DigitsField(value: String, onChange: (String) -> Unit, label: String) =
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())

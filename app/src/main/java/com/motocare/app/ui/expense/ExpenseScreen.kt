package com.motocare.app.ui.expense

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.asPeso
import java.time.LocalDate

private val expenseCategories = listOf("FUEL", "PARKING", "MAINTENANCE", "REPAIRS", "REGISTRATION", "INSURANCE", "ACCESSORIES", "LOAN_PAYMENT", "FINES", "OTHER")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(onBack: () -> Unit, startWithParking: Boolean = false, viewModel: ExpenseViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showParkingDefault by remember { mutableStateOf(false) }
    var parkingHandled by remember { mutableStateOf(false) }
    if (startWithParking && !parkingHandled && state.motorcycle != null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.addParking(); parkingHandled = true }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Expenses") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ showAdd = true }) { Icon(Icons.Outlined.Add, "Add expense") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(state.motorcycle?.name ?: "No motorcycle selected", style = MaterialTheme.typography.titleLarge)
                Text("Today ${state.todayTotalCentavos.asPeso()} • Month ${state.monthTotalCentavos.asPeso()} • Year ${state.yearTotalCentavos.asPeso()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                AssistChip(
                    onClick = viewModel::addParking,
                    label = { Text("Add today’s parking • ${state.defaultParkingCentavos.asPeso()}") },
                    leadingIcon = { Icon(Icons.Outlined.LocalParking, contentDescription = null) },
                )
                TextButton(onClick = { showParkingDefault = true }) { Text("Change parking default") }
            }
            if (state.expenses.isEmpty()) item { Text("No expenses yet. Use the parking shortcut or add a cost.") }
            items(state.expenses, key = { it.id }) { expense ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(expense.category.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                            Text(LocalDate.ofEpochDay(expense.dateEpochDay).asDisplayDate())
                            if (expense.description.isNotBlank()) Text(expense.description)
                        }
                        Text(expense.amountCentavos.asPeso(), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
    if (showAdd) AddExpenseDialog(onDismiss = { showAdd = false }, onSave = { viewModel.add(it) { showAdd = false } })
    if (showParkingDefault) ParkingDefaultDialog(
        current = state.defaultParkingCentavos,
        onDismiss = { showParkingDefault = false },
        onSave = { viewModel.setParkingDefault(it); showParkingDefault = false },
    )
}

@Composable
private fun ParkingDefaultDialog(current: Long, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var amount by remember(current) { mutableStateOf("%.2f".format(current / 100.0)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default parking amount") },
        text = { OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (PHP)") }) },
        confirmButton = { TextButton(enabled = amount.toBigDecimalOrNull() != null, onClick = { onSave(amount) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (ExpenseInput) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var category by remember { mutableStateOf("OTHER") }
    var amount by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf<String?>(null) }
    var payment by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            receipt = uri.toString()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Text("Category", fontWeight = FontWeight.SemiBold)
                expenseCategories.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { option -> AssistChip(onClick = { category = option }, label = { Text(if (category == option) "✓ ${option.lowercase()}" else option.lowercase()) }) }
                    }
                }
                OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (PHP)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Odometer (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(payment, { payment = it }, label = { Text("Payment method") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(vendor, { vendor = it }, label = { Text("Vendor") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(if (receipt == null) "Attach receipt photo" else "Receipt selected") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.toBigDecimalOrNull() != null && runCatching { LocalDate.parse(date) }.isSuccess,
                onClick = { onSave(ExpenseInput(date, category, amount, km, description, receipt, payment, vendor)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

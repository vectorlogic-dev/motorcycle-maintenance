package com.motocare.app.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.asPeso
import com.motocare.app.ui.components.MotoCareEmptyState
import com.motocare.app.ui.components.MotoCareSummaryCard
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelScreen(onBack: () -> Unit, viewModel: FuelViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Fuel") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ showAdd = true }) { Icon(Icons.Outlined.Add, "Add fuel") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(state.motorcycle?.name ?: "No motorcycle selected", style = MaterialTheme.typography.titleLarge)
                val best = state.summary.bestKmPerLitre?.let { "${"%.1f".format(it)}" } ?: "—"
                val worst = state.summary.worstKmPerLitre?.let { "${"%.1f".format(it)}" } ?: "—"
                MotoCareSummaryCard(
                    label = "Average economy",
                    value = state.summary.averageKmPerLitre?.let { "${"%.1f".format(it)} km/L" } ?: "Add two full tanks",
                    detail = "This month ${(state.summary.monthlySpendingCentavos[YearMonth.now().toString()] ?: 0).asPeso()} • Best $best • Worst $worst km/L" +
                        (state.summary.fuelCostPerKmCentavos?.let { "\nFuel cost ${(it.toLong()).asPeso()}/km" } ?: ""),
                    icon = Icons.Outlined.LocalGasStation,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (state.entries.isEmpty()) item {
                MotoCareEmptyState(
                    title = "No fuel entries yet",
                    detail = "Economy is calculated between valid full-tank entries.",
                    icon = Icons.Outlined.LocalGasStation,
                    actionLabel = "Add fuel",
                    onAction = { showAdd = true },
                )
            }
            items(state.entries, key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${"%.2f".format(entry.litres)} L${if (entry.fullTank) " • Full" else ""}", fontWeight = FontWeight.SemiBold)
                            Text("${"%,d".format(entry.odometerKm)} km • ${LocalDate.ofEpochDay(entry.dateEpochDay).asDisplayDate()}")
                            if (entry.station.isNotBlank()) Text(entry.station)
                        }
                        Text(entry.totalCostCentavos.asPeso(), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
    if (showAdd) AddFuelDialog(
        currentKm = state.motorcycle?.currentOdometerKm ?: 0,
        defaultPrice = state.defaultPriceCentavos / 100.0,
        onDismiss = { showAdd = false },
        onSave = { viewModel.add(it) { showAdd = false } },
    )
}

@Composable
private fun AddFuelDialog(currentKm: Long, defaultPrice: Double, onDismiss: () -> Unit, onSave: (FuelInput) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var km by remember { mutableStateOf(currentKm.toString()) }
    var litres by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("%.2f".format(defaultPrice)) }
    var full by remember { mutableStateOf(true) }
    var station by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val total = (litres.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add fuel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Odometer (km)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(litres, { litres = decimalOnly(it) }, label = { Text("Litres") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = decimalOnly(it) }, label = { Text("Price per litre (PHP)") }, modifier = Modifier.fillMaxWidth())
                Text("Total: ${((total * 100).toLong()).asPeso()}", fontWeight = FontWeight.SemiBold)
                Row { Checkbox(full, { full = it }); Text("Full tank", Modifier.padding(top = 12.dp)) }
                OutlinedTextField(station, { station = it }, label = { Text("Fuel station") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = km.toLongOrNull() != null && litres.toDoubleOrNull()?.let { it > 0 } == true && price.toDoubleOrNull()?.let { it > 0 } == true && runCatching { LocalDate.parse(date) }.isSuccess,
                onClick = { onSave(FuelInput(date, km, litres, price, full, station, notes)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun decimalOnly(value: String): String = value.filter { it.isDigit() || it == '.' }

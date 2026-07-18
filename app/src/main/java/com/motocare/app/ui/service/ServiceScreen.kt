package com.motocare.app.ui.service

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.asPeso
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(onBack: () -> Unit, viewModel: ServiceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Service history") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ showAdd = true }) { Icon(Icons.Outlined.Add, "Add service") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(state.motorcycle?.name ?: "No motorcycle selected", style = MaterialTheme.typography.titleLarge) }
            if (state.records.isEmpty()) item { Text("No service records yet. Completed maintenance will appear here.") }
            items(state.records, key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(LocalDate.ofEpochDay(record.serviceEpochDay).asDisplayDate(), fontWeight = FontWeight.SemiBold)
                            Text((record.labourCostCentavos + record.partsCostCentavos).asPeso(), color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${"%,d".format(record.odometerKm)} km")
                        if (record.dealerOrMechanic.isNotBlank()) Text(record.dealerOrMechanic)
                        if (record.partsReplaced.isNotBlank()) Text("Parts: ${record.partsReplaced}")
                        if (record.notes.isNotBlank()) Text(record.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (showAdd) AddServiceDialog(
        currentKm = state.motorcycle?.currentOdometerKm ?: 0,
        schedules = state.schedules,
        onDismiss = { showAdd = false },
        onSave = { viewModel.add(it) { showAdd = false } },
    )
}

@Composable
private fun AddServiceDialog(
    currentKm: Long,
    schedules: List<MaintenanceScheduleEntity>,
    onDismiss: () -> Unit,
    onSave: (ServiceInput) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var km by remember { mutableStateOf(currentKm.toString()) }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var mechanic by remember { mutableStateOf("") }
    var labour by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    var replaced by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var receipts by remember { mutableStateOf(emptyList<String>()) }
    var nextDate by remember { mutableStateOf("") }
    var nextKm by remember { mutableStateOf("") }
    val context = LocalContext.current
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        receipts = uris.map { it.toString() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add service record") },
        text = {
            Column(Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(date, { date = it }, "Service date (YYYY-MM-DD)")
                NumberField(km, { km = it }, "Odometer (km)")
                Text("Maintenance completed", fontWeight = FontWeight.SemiBold)
                schedules.forEach { schedule ->
                    Row(Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = schedule.id in selected,
                            onCheckedChange = { checked -> selected = if (checked) selected + schedule.id else selected - schedule.id },
                        )
                        Text(schedule.name, Modifier.padding(top = 12.dp))
                    }
                }
                Field(mechanic, { mechanic = it }, "Dealer or mechanic")
                NumberField(labour, { labour = it }, "Labour cost (PHP)", allowDecimal = true)
                NumberField(parts, { parts = it }, "Parts cost (PHP)", allowDecimal = true)
                Field(replaced, { replaced = it }, "Parts replaced")
                Field(notes, { notes = it }, "Notes", false)
                TextButton(onClick = { receiptPicker.launch(arrayOf("image/*")) }) {
                    Text(if (receipts.isEmpty()) "Attach receipt photos" else "${receipts.size} receipt photo(s) selected")
                }
                Field(nextDate, { nextDate = it }, "Next recommended date")
                NumberField(nextKm, { nextKm = it }, "Next recommended odometer")
            }
        },
        confirmButton = {
            TextButton(
                enabled = km.toLongOrNull() != null && runCatching { LocalDate.parse(date) }.isSuccess,
                onClick = { onSave(ServiceInput(date, km, selected, mechanic, labour, parts, replaced, notes, receipts, nextDate, nextKm)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) =
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine)

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, allowDecimal: Boolean = false) =
    Field(value, { text -> onChange(text.filter { it.isDigit() || (allowDecimal && it == '.') }) }, label)

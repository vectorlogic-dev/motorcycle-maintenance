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
import androidx.compose.material.icons.outlined.Build
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
import androidx.compose.runtime.LaunchedEffect
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
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.asPeso
import com.motocare.app.ui.components.MotoCareEmptyState
import com.motocare.app.ui.components.MotoCareLoadingState
import com.motocare.app.ui.components.MotoCareNoMotorcycleState
import com.motocare.app.ui.components.MotoCareDateField
import com.motocare.app.ui.components.MotoCareOptionalDateField
import com.motocare.app.ui.components.MotoCareDeleteDialog
import com.motocare.app.ui.components.MotoCareRecordActions
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(
    onBack: () -> Unit,
    onManageMotorcycles: () -> Unit,
    startWithAdd: Boolean = false,
    viewModel: ServiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var startAddHandled by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ServiceRecordEntity?>(null) }
    var editingItemIds by remember { mutableStateOf(emptySet<Long>()) }
    var deleteTarget by remember { mutableStateOf<ServiceRecordEntity?>(null) }
    LaunchedEffect(startWithAdd, state.motorcycle?.id) {
        if (startWithAdd && !startAddHandled && state.motorcycle != null) {
            showAdd = true
            startAddHandled = true
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Service history") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ showAdd = true }) { Icon(Icons.Outlined.Add, "Add service") } },
    ) { padding ->
        when {
            state.isLoading -> MotoCareLoadingState(Modifier.padding(padding))
            state.motorcycle == null -> MotoCareNoMotorcycleState(onManageMotorcycles, Modifier.padding(padding))
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(state.motorcycle?.name.orEmpty(), style = MaterialTheme.typography.titleLarge) }
            if (state.records.isEmpty()) item {
                MotoCareEmptyState(
                    title = "No service records yet",
                    detail = "Log completed work to build a useful maintenance history.",
                    icon = Icons.Outlined.Build,
                    actionLabel = "Add service",
                    onAction = { showAdd = true },
                )
            }
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
                        MotoCareRecordActions(
                            "service record",
                            onEdit = { viewModel.loadItemIds(record.id) { ids -> editingItemIds = ids; editing = record } },
                            onDelete = { deleteTarget = record },
                        )
                    }
                }
            }
        }
        }
    }
    if (showAdd || editing != null) AddServiceDialog(
        existing = editing,
        initialScheduleIds = editingItemIds,
        currentKm = state.motorcycle?.currentOdometerKm ?: 0,
        schedules = state.schedules,
        onDismiss = { showAdd = false; editing = null; editingItemIds = emptySet() },
        onSave = { input ->
            val existing = editing
            if (existing == null) viewModel.add(input) { showAdd = false } else viewModel.update(existing, input) { editing = null; editingItemIds = emptySet() }
        },
    )
    deleteTarget?.let { record ->
        MotoCareDeleteDialog(
            title = "Delete service record?",
            detail = "Delete the service from ${LocalDate.ofEpochDay(record.serviceEpochDay).asDisplayDate()}? Related maintenance status and odometer history will be recalculated.",
            onConfirm = { viewModel.delete(record); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun AddServiceDialog(
    existing: ServiceRecordEntity?,
    initialScheduleIds: Set<Long>,
    currentKm: Long,
    schedules: List<MaintenanceScheduleEntity>,
    onDismiss: () -> Unit,
    onSave: (ServiceInput) -> Unit,
) {
    var date by remember(existing) { mutableStateOf(existing?.serviceEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()) }
    var km by remember(existing) { mutableStateOf(existing?.odometerKm?.toString() ?: currentKm.toString()) }
    var selected by remember(existing, initialScheduleIds) { mutableStateOf(initialScheduleIds) }
    var mechanic by remember(existing) { mutableStateOf(existing?.dealerOrMechanic.orEmpty()) }
    var labour by remember(existing) { mutableStateOf(existing?.labourCostCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var parts by remember(existing) { mutableStateOf(existing?.partsCostCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var replaced by remember(existing) { mutableStateOf(existing?.partsReplaced.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var receipts by remember(existing) { mutableStateOf(emptyList<String>()) }
    var nextDate by remember(existing) { mutableStateOf(existing?.nextRecommendedEpochDay?.let(LocalDate::ofEpochDay)) }
    var nextKm by remember(existing) { mutableStateOf(existing?.nextRecommendedOdometerKm?.toString().orEmpty()) }
    val context = LocalContext.current
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        receipts = uris.map { it.toString() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add service record" else "Edit service record") },
        text = {
            Column(Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MotoCareDateField(date, { date = it }, label = "Service date")
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
                MotoCareOptionalDateField(nextDate, { nextDate = it }, "Next recommended date")
                NumberField(nextKm, { nextKm = it }, "Next recommended odometer")
            }
        },
        confirmButton = {
            TextButton(
                enabled = km.toLongOrNull() != null,
                onClick = { onSave(ServiceInput(date.toString(), km, selected, mechanic, labour, parts, replaced, notes, receipts, nextDate?.toString().orEmpty(), nextKm)) },
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

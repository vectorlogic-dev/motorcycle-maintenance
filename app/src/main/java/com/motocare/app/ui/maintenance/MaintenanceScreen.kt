package com.motocare.app.ui.maintenance

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.ui.components.MotoCareEmptyState
import com.motocare.app.ui.components.MotoCareLoadingState
import com.motocare.app.ui.components.MotoCareNoMotorcycleState
import com.motocare.app.ui.components.MotoCareStatusPill
import com.motocare.app.ui.components.MotoCareOptionalDateField
import com.motocare.app.ui.theme.motoCareStatusColors
import com.motocare.app.ui.dashboard.ScheduleRow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    contentPadding: PaddingValues,
    onManageMotorcycles: () -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MaintenanceScheduleEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deactivate by remember { mutableStateOf<MaintenanceScheduleEntity?>(null) }
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text("Maintenance") }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ adding = true }) { Icon(Icons.Outlined.Add, "Add maintenance item") } },
    ) { inner ->
        when {
            state.isLoading -> MotoCareLoadingState(Modifier.padding(inner))
            state.motorcycle == null -> MotoCareNoMotorcycleState(onManageMotorcycles, Modifier.padding(inner))
            else -> LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(state.motorcycle?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Text("Mileage and time intervals are evaluated together; the first one reached determines the status.")
            }
            if (state.schedules.isEmpty()) item {
                MotoCareEmptyState(
                    title = "No maintenance items yet",
                    detail = "Add an editable schedule to keep upcoming service visible.",
                    icon = Icons.Outlined.Build,
                    actionLabel = "Add maintenance item",
                    onAction = { adding = true },
                )
            }
            items(state.schedules, key = { it.schedule.id }) { row ->
                MaintenanceCard(row, onEdit = { editing = row.schedule }, onDeactivate = { deactivate = row.schedule })
            }
        }
        }
    }
    if (adding || editing != null) {
        ScheduleDialog(
            existing = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { viewModel.save(it); adding = false; editing = null },
        )
    }
    deactivate?.let { schedule ->
        AlertDialog(
            onDismissRequest = { deactivate = null },
            title = { Text("Deactivate ${schedule.name}?") },
            text = { Text("It will stop appearing in due calculations. Existing service history is not deleted.") },
            confirmButton = { TextButton(onClick = { viewModel.deactivate(schedule.id); deactivate = null }) { Text("Deactivate") } },
            dismissButton = { TextButton(onClick = { deactivate = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MaintenanceCard(row: ScheduleRow, onEdit: () -> Unit, onDeactivate: () -> Unit) {
    val statusColors = MaterialTheme.motoCareStatusColors
    val (statusColor, statusContainer) = when (row.assessment.status) {
        MaintenanceStatus.GOOD -> statusColors.success to statusColors.successContainer
        MaintenanceStatus.DUE_SOON -> statusColors.warning to statusColors.warningContainer
        MaintenanceStatus.DUE -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
        MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
    }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(row.schedule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                MotoCareStatusPill(
                    row.assessment.status.name.replace('_', ' '),
                    statusColor,
                    statusContainer,
                    Modifier.padding(vertical = 6.dp),
                )
                val remaining = listOfNotNull(
                    row.assessment.remainingKm?.let { "$it km" },
                    row.assessment.remainingDays?.let { "$it days" },
                ).joinToString(" or ")
                Text(remaining.ifEmpty { "Open to set an interval" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (row.schedule.isEditableTemplate) Text("Editable starter template", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDeactivate) { Icon(Icons.Outlined.DeleteOutline, "Deactivate ${row.schedule.name}") }
        }
    }
}

@Composable
private fun ScheduleDialog(existing: MaintenanceScheduleEntity?, onDismiss: () -> Unit, onSave: (ScheduleInput) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var intervalKm by remember(existing) { mutableStateOf(existing?.intervalKm?.toString().orEmpty()) }
    var intervalDays by remember(existing) { mutableStateOf(existing?.intervalDays?.toString().orEmpty()) }
    var lastDate by remember(existing) { mutableStateOf(existing?.lastServiceEpochDay?.let(LocalDate::ofEpochDay)) }
    var lastKm by remember(existing) { mutableStateOf(existing?.lastServiceOdometerKm?.toString().orEmpty()) }
    var leadDays by remember(existing) { mutableStateOf(existing?.reminderLeadDays?.toString() ?: "14") }
    var leadKm by remember(existing) { mutableStateOf(existing?.reminderLeadKm?.toString() ?: "500") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add maintenance item" else "Edit maintenance item") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, "Name")
                Field(description, { description = it }, "Description", false)
                Text("Due every (use one or both)", fontWeight = FontWeight.SemiBold)
                NumberField(intervalKm, { intervalKm = it }, "Kilometres")
                NumberField(intervalDays, { intervalDays = it }, "Days")
                Text("Last completed", fontWeight = FontWeight.SemiBold)
                MotoCareOptionalDateField(lastDate, { lastDate = it }, "Last service date")
                NumberField(lastKm, { lastKm = it }, "Odometer (km)")
                Text("Reminder lead", fontWeight = FontWeight.SemiBold)
                NumberField(leadDays, { leadDays = it }, "Days before")
                NumberField(leadKm, { leadKm = it }, "Kilometres before")
                Text("Confirm intervals against the motorcycle owner’s manual or dealer booklet.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (intervalKm.toLongOrNull() != null || intervalDays.toIntOrNull() != null),
                onClick = { onSave(ScheduleInput(existing, name, description, intervalKm, intervalDays, lastDate?.toString().orEmpty(), lastKm, leadDays, leadKm)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine)
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) =
    Field(value, { onChange(it.filter(Char::isDigit)) }, label)

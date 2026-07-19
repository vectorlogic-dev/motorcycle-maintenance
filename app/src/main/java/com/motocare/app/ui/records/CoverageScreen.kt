package com.motocare.app.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import com.motocare.app.util.asDisplayDate
import com.motocare.app.ui.components.MotoCareDateField
import com.motocare.app.ui.components.MotoCareLoadingState
import com.motocare.app.ui.components.MotoCareNoMotorcycleState
import java.time.LocalDate
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverageScreen(
    onBack: () -> Unit,
    onManageMotorcycles: () -> Unit,
    viewModel: ComplianceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Maintenance coverage") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        when {
            state.isLoading -> MotoCareLoadingState(Modifier.padding(padding))
            state.motorcycle == null -> MotoCareNoMotorcycleState(onManageMotorcycles, Modifier.padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(state.motorcycle?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            val plan = state.coverage
            if (plan == null) {
                Text("No coverage plan recorded.")
                Button(onClick = { editing = true }) { Text("Add coverage") }
            } else {
                state.coverageAssessment?.let { assessment ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("WHICHEVER COMES FIRST", style = MaterialTheme.typography.labelLarge)
                            Text("${max(0, assessment.remainingDays)} days", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("or ${"%,d".format(max(0, assessment.remainingKm))} km remaining")
                            assessment.estimatedEndDate?.let { Text("Estimated end ${it.asDisplayDate()}") }
                        }
                    }
                }
                Text("${LocalDate.ofEpochDay(plan.startEpochDay).asDisplayDate()} – ${LocalDate.ofEpochDay(plan.endEpochDay).asDisplayDate()}")
                Text("Dealer: ${plan.dealerName.ifBlank { "Not set" }}")
                Text("Labour ${if (plan.coveredLabour) "covered" else "not marked covered"} • Parts ${if (plan.coveredParts) "covered" else "not marked covered"}")
                if (plan.coveredServices.isNotBlank()) Text(plan.coveredServices)
                Text("Upcoming services before coverage ends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (state.upcomingCoveredSchedules.isEmpty()) Text("No scheduled items currently fall within the recorded limits.")
                state.upcomingCoveredSchedules.forEach { Text("• ${it.name}") }
                Button(onClick = { editing = true }) { Text("Edit coverage") }
            }
        }
        }
    }
    if (editing) CoverageDialog(state.coverage, state.motorcycle?.initialOdometerKm ?: 0, onDismiss = { editing = false }, onSave = { viewModel.saveCoverage(it) { editing = false } })
}

@Composable
private fun CoverageDialog(existing: com.motocare.app.data.local.entity.CoveragePlanEntity?, initialKm: Long, onDismiss: () -> Unit, onSave: (CoverageInput) -> Unit) {
    val today = LocalDate.now()
    var start by remember(existing) { mutableStateOf(existing?.startEpochDay?.let(LocalDate::ofEpochDay) ?: today) }
    var end by remember(existing) { mutableStateOf(existing?.endEpochDay?.let(LocalDate::ofEpochDay) ?: today.plusYears(1)) }
    var startKm by remember(existing) { mutableStateOf(existing?.startOdometerKm?.toString() ?: initialKm.toString()) }
    var limit by remember(existing) { mutableStateOf(existing?.limitOdometerKm?.toString() ?: "12000") }
    var services by remember(existing) { mutableStateOf(existing?.coveredServices.orEmpty()) }
    var labour by remember(existing) { mutableStateOf(existing?.coveredLabour ?: false) }
    var parts by remember(existing) { mutableStateOf(existing?.coveredParts ?: false) }
    var dealer by remember(existing) { mutableStateOf(existing?.dealerName.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coverage details") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MotoCareDateField(start, { start = it }, label = "Coverage start")
                MotoCareDateField(end, { end = it }, label = "Coverage end")
                NumberField(startKm, { startKm = it }, "Starting odometer")
                NumberField(limit, { limit = it }, "Coverage kilometre limit")
                Field(services, { services = it }, "Covered services", false)
                Row { Checkbox(labour, { labour = it }); Text("Labour covered", Modifier.padding(top = 12.dp)) }
                Row { Checkbox(parts, { parts = it }); Text("Parts covered", Modifier.padding(top = 12.dp)) }
                Field(dealer, { dealer = it }, "Dealer")
                Field(notes, { notes = it }, "Notes", false)
            }
        },
        confirmButton = { TextButton(enabled = end >= start, onClick = { onSave(CoverageInput(start.toString(), end.toString(), startKm, limit, services, labour, parts, dealer, notes)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) = OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine)
@Composable private fun NumberField(value: String, onChange: (String) -> Unit, label: String) = Field(value, { onChange(it.filter(Char::isDigit)) }, label)

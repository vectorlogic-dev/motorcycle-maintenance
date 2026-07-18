package com.motocare.app.ui.problem

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ReportProblem
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
import com.motocare.app.data.local.entity.ProblemLogEntity
import com.motocare.app.ui.components.MotoCareEmptyState
import com.motocare.app.ui.components.MotoCareStatusPill
import com.motocare.app.ui.theme.motoCareStatusColors
import com.motocare.app.util.asDisplayDate
import java.time.LocalDate

private val severities = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemScreen(onBack: () -> Unit, viewModel: ProblemViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf<ProblemLogEntity?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Problems & symptoms") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ adding = true }) { Icon(Icons.Outlined.Add, "Log issue") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(state.motorcycle?.name ?: "No motorcycle selected", style = MaterialTheme.typography.titleLarge) }
            if (state.problems.isEmpty()) item {
                MotoCareEmptyState(
                    title = "No issues logged",
                    detail = "Problems you log here stay visible on the dashboard until resolved.",
                    icon = Icons.Outlined.ReportProblem,
                    actionLabel = "Log an issue",
                    onAction = { adding = true },
                )
            }
            items(state.problems, key = { it.id }) { problem ->
                Card(Modifier.fillMaxWidth().clickable(enabled = !problem.resolved) { resolving = problem }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(problem.symptom, fontWeight = FontWeight.SemiBold)
                            val statusColors = MaterialTheme.motoCareStatusColors
                            MotoCareStatusPill(
                                if (problem.resolved) "RESOLVED" else problem.severity,
                                if (problem.resolved) statusColors.success else MaterialTheme.colorScheme.error,
                                if (problem.resolved) statusColors.successContainer else MaterialTheme.colorScheme.errorContainer,
                            )
                        }
                        Text(LocalDate.ofEpochDay(problem.dateEpochDay).asDisplayDate() + (problem.odometerKm?.let { " • $it km" } ?: ""))
                        if (problem.description.isNotBlank()) Text(problem.description)
                        if (problem.resolved && problem.resolution.isNotBlank()) Text("Resolution: ${problem.resolution}")
                    }
                }
            }
        }
    }
    if (adding) AddProblemDialog(state.motorcycle?.currentOdometerKm ?: 0, { adding = false }) { viewModel.add(it) { adding = false } }
    resolving?.let { problem -> ResolveDialog(problem, { resolving = null }) { viewModel.resolve(problem, it); resolving = null } }
}

@Composable
private fun AddProblemDialog(currentKm: Long, onDismiss: () -> Unit, onSave: (ProblemInput) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }; var km by remember { mutableStateOf(currentKm.toString()) }
    var severity by remember { mutableStateOf("MEDIUM") }; var symptom by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var media by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; media = uri.toString() }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Log issue") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Odometer (km)") }, modifier = Modifier.fillMaxWidth())
            severities.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { row.forEach { option -> AssistChip(onClick = { severity = option }, label = { Text(if (severity == option) "✓ ${option.lowercase()}" else option.lowercase()) }) } }
            }
            OutlinedTextField(symptom, { symptom = it }, label = { Text("Symptom") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { picker.launch(arrayOf("image/*", "video/*")) }) { Text(if (media == null) "Attach photo or video" else "Media selected") }
        }
    }, confirmButton = { TextButton(enabled = symptom.isNotBlank() && runCatching { LocalDate.parse(date) }.isSuccess, onClick = { onSave(ProblemInput(date, km, severity, symptom, description, media)) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ResolveDialog(problem: ProblemLogEntity, onDismiss: () -> Unit, onResolve: (String) -> Unit) {
    var resolution by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Resolve ${problem.symptom}") }, text = { OutlinedTextField(resolution, { resolution = it }, label = { Text("Resolution") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(enabled = resolution.isNotBlank(), onClick = { onResolve(resolution) }) { Text("Mark resolved") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

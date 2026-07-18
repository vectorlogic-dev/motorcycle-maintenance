package com.motocare.app.ui.odometer

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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerScreen(onBack: () -> Unit, viewModel: OdometerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Odometer") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { if (state.motorcycle != null) FloatingActionButton({ showAdd = true }) { Icon(Icons.Outlined.Add, "Add reading") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(state.motorcycle?.name ?: "No motorcycle selected", style = MaterialTheme.typography.titleLarge)
                Text("Average ${"%.1f".format(state.stats.averageKmPerDay)} km/day • ${"%.0f".format(state.stats.averageKmPerMonth)} km/month")
            }
            if (state.entries.isEmpty()) item { Text("No readings yet. Add the current odometer to begin.") }
            items(state.entries, key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${"%,d".format(entry.readingKm)} km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            if (entry.note.isNotBlank()) Text(entry.note)
                        }
                        Column {
                            val date = Instant.ofEpochMilli(entry.recordedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                            Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            if (entry.isCorrection) Text("Correction", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
    if (showAdd) AddReadingDialog(
        currentKm = state.motorcycle?.currentOdometerKm ?: 0,
        error = state.error,
        onDismiss = { showAdd = false },
        onAdd = { km, date, note -> viewModel.add(km, date, note) { showAdd = false } },
    )
    state.correctionPreviousKm?.let { previous ->
        AlertDialog(
            onDismissRequest = viewModel::cancelCorrection,
            title = { Text("Confirm odometer correction") },
            text = { Text("This reading is lower than the current ${"%,d".format(previous)} km. Save it as an explicit correction?") },
            confirmButton = { TextButton(onClick = { viewModel.confirmCorrection { showAdd = false } }) { Text("Save correction") } },
            dismissButton = { TextButton(onClick = viewModel::cancelCorrection) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddReadingDialog(currentKm: Long, error: String?, onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var km by remember { mutableStateOf(currentKm.toString()) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add odometer reading") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(km, { km = it.filter(Char::isDigit) }, label = { Text("Reading (km)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(km, date, note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

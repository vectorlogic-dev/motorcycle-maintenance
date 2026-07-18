package com.motocare.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmRestore by remember { mutableStateOf(false) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(viewModel::backup) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::restore) }
    val expenseCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { uri -> viewModel.export(uri, "expenses") } }
    val fuelCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { uri -> viewModel.export(uri, "fuel_entries") } }
    val serviceCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { uri -> viewModel.export(uri, "service_records") } }
    val odometerCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { uri -> viewModel.export(uri, "odometer_entries") } }
    val suffix = LocalDate.now().toString()
    Scaffold(topBar = { TopAppBar(title = { Text("Backup & export") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.Start) {
            Text("Full backup", style = MaterialTheme.typography.titleLarge)
            Text("JSON backups contain all MotoCare records and URI references. Image and video files themselves are not copied.")
            Button(enabled = !state.working, onClick = { backup.launch("motocare-backup-$suffix.json") }, modifier = Modifier.fillMaxWidth()) { Text("Create JSON backup") }
            OutlinedButton(enabled = !state.working, onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Restore JSON backup") }
            Text("CSV exports", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
            ExportButton("Expenses") { expenseCsv.launch("motocare-expenses-$suffix.csv") }
            ExportButton("Fuel") { fuelCsv.launch("motocare-fuel-$suffix.csv") }
            ExportButton("Service history") { serviceCsv.launch("motocare-services-$suffix.csv") }
            ExportButton("Odometer readings") { odometerCsv.launch("motocare-odometer-$suffix.csv") }
            if (state.working) CircularProgressIndicator()
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("Replace all MotoCare data?") },
        text = { Text("Restore deletes the current on-device records and replaces them with the selected backup. Create a current backup first if needed.") },
        confirmButton = { TextButton(onClick = { confirmRestore = false; restore.launch(arrayOf("application/json", "text/plain")) }) { Text("Choose backup") } },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit) = OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Export $label") }

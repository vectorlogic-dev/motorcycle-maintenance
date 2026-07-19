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
import com.motocare.app.data.local.entity.InsuranceRecordEntity
import com.motocare.app.data.local.entity.RegistrationRecordEntity
import com.motocare.app.util.asDisplayDate
import com.motocare.app.ui.components.MotoCareOptionalDateField
import com.motocare.app.ui.components.MotoCareLoadingState
import com.motocare.app.ui.components.MotoCareNoMotorcycleState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    onManageMotorcycles: () -> Unit,
    viewModel: ComplianceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editRegistration by remember { mutableStateOf(false) }
    var editInsurance by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Registration & insurance") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        when {
            state.isLoading -> MotoCareLoadingState(Modifier.padding(padding))
            state.motorcycle == null -> MotoCareNoMotorcycleState(onManageMotorcycles, Modifier.padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(state.motorcycle?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            RecordCard("Registration", state.registration?.expiryEpochDay, listOfNotNull(
                state.registration?.plateNumber?.takeIf { it.isNotBlank() }?.let { "Plate $it" },
                state.registration?.ltoTransactionReference?.takeIf { it.isNotBlank() }?.let { "Reference $it" },
                state.registration?.temporaryPlate?.takeIf { it }?.let { "Temporary plate" },
            ), { editRegistration = true })
            RecordCard("Insurance", state.insurance?.expiryEpochDay, listOfNotNull(
                state.insurance?.provider?.takeIf { it.isNotBlank() },
                state.insurance?.policyNumber?.takeIf { it.isNotBlank() }?.let { "Policy $it" },
            ), { editInsurance = true })
            Text("MotoCare stores dates, references, and your notes. It does not determine legal compliance.", style = MaterialTheme.typography.bodySmall)
        }
        }
    }
    if (editRegistration) RegistrationDialog(state.registration, state.motorcycle?.plateNumber.orEmpty(), state.motorcycle?.registrationExpiryEpochDay, { editRegistration = false }) { viewModel.saveRegistration(it) { editRegistration = false } }
    if (editInsurance) InsuranceDialog(state.insurance, state.motorcycle?.insuranceExpiryEpochDay, { editInsurance = false }) { viewModel.saveInsurance(it) { editInsurance = false } }
}

@Composable
private fun RecordCard(title: String, expiryEpochDay: Long?, details: List<String>, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (expiryEpochDay == null) Text("Expiry not recorded") else {
                val expiry = LocalDate.ofEpochDay(expiryEpochDay)
                val days = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                Text("Expires ${expiry.asDisplayDate()} • $days days", color = if (days < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            details.forEach { Text(it) }
            Button(onClick = onEdit) { Text("${if (expiryEpochDay == null) "Add" else "Edit"} $title") }
        }
    }
}

@Composable
private fun RegistrationDialog(existing: RegistrationRecordEntity?, defaultPlate: String, defaultExpiry: Long?, onDismiss: () -> Unit, onSave: (RegistrationInput) -> Unit) {
    fun date(epoch: Long?) = epoch?.let(LocalDate::ofEpochDay)
    var orDate by remember(existing) { mutableStateOf(date(existing?.orDateEpochDay)) }; var crDate by remember(existing) { mutableStateOf(date(existing?.crDateEpochDay)) }
    var expiry by remember(existing) { mutableStateOf(date(existing?.expiryEpochDay ?: defaultExpiry)) }; var plate by remember(existing) { mutableStateOf(existing?.plateNumber ?: defaultPlate) }
    var temporary by remember(existing) { mutableStateOf(existing?.temporaryPlate ?: false) }; var submission by remember(existing) { mutableStateOf(date(existing?.dealerSubmissionEpochDay)) }
    var reference by remember(existing) { mutableStateOf(existing?.ltoTransactionReference.orEmpty()) }; var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Registration record") }, text = {
        Column(Modifier.heightIn(max = 550.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MotoCareOptionalDateField(orDate, { orDate = it }, "OR date"); MotoCareOptionalDateField(crDate, { crDate = it }, "CR date"); MotoCareOptionalDateField(expiry, { expiry = it }, "Registration expiry")
            DocField(plate, { plate = it }, "Plate number"); Row { Checkbox(temporary, { temporary = it }); Text("Temporary plate", Modifier.padding(top = 12.dp)) }
            MotoCareOptionalDateField(submission, { submission = it }, "Dealer submission date"); DocField(reference, { reference = it }, "LTO transaction reference"); DocField(notes, { notes = it }, "Notes", false)
        }
    }, confirmButton = { TextButton(onClick = { onSave(RegistrationInput(orDate?.toString().orEmpty(), crDate?.toString().orEmpty(), expiry?.toString().orEmpty(), plate, temporary, submission?.toString().orEmpty(), reference, notes)) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun InsuranceDialog(existing: InsuranceRecordEntity?, defaultExpiry: Long?, onDismiss: () -> Unit, onSave: (InsuranceInput) -> Unit) {
    fun date(epoch: Long?) = epoch?.let(LocalDate::ofEpochDay)
    var provider by remember(existing) { mutableStateOf(existing?.provider.orEmpty()) }; var policy by remember(existing) { mutableStateOf(existing?.policyNumber.orEmpty()) }
    var start by remember(existing) { mutableStateOf(date(existing?.startEpochDay)) }; var expiry by remember(existing) { mutableStateOf(date(existing?.expiryEpochDay ?: defaultExpiry)) }; var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Insurance record") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { DocField(provider, { provider = it }, "Provider"); DocField(policy, { policy = it }, "Policy number"); MotoCareOptionalDateField(start, { start = it }, "Start date"); MotoCareOptionalDateField(expiry, { expiry = it }, "Expiry date"); DocField(notes, { notes = it }, "Notes", false) }
    }, confirmButton = { TextButton(onClick = { onSave(InsuranceInput(provider, policy, start?.toString().orEmpty(), expiry?.toString().orEmpty(), notes)) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun DocField(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) = OutlinedTextField(value, onChange, label = { Text(if ("date" in label.lowercase() || "expiry" in label.lowercase()) "$label (YYYY-MM-DD)" else label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine)

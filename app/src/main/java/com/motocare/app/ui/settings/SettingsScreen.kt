package com.motocare.app.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class SettingsActions(
    val setCurrency: (String) -> Unit = {},
    val setDateFormat: (String) -> Unit = {},
    val setTheme: (String) -> Unit = {},
    val setNotificationsEnabled: (Boolean) -> Unit = {},
    val setMotorcycleNotificationsEnabled: (Long, Boolean) -> Unit = { _, _ -> },
    val saveReminderDays: (String) -> Unit = {},
    val saveDefaults: (String, String) -> Unit = { _, _ -> },
    val clearMessage: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        contentPadding = contentPadding,
        versionName = appVersion(LocalContext.current),
        onBackup = onBackup,
        actions = SettingsActions(
            viewModel::setCurrency,
            viewModel::setDateFormat,
            viewModel::setTheme,
            viewModel::setNotificationsEnabled,
            viewModel::setMotorcycleNotificationsEnabled,
            viewModel::saveReminderDays,
            viewModel::saveDefaults,
            viewModel::clearMessage,
        ),
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    versionName: String,
    onBackup: () -> Unit,
    actions: SettingsActions,
) {
    var parking by remember { mutableStateOf(centavosText(state.defaultParkingCentavos)) }
    var fuel by remember { mutableStateOf(centavosText(state.defaultFuelPriceCentavos)) }
    var staleDays by remember { mutableStateOf(state.staleOdometerDays.toString()) }
    LaunchedEffect(state.defaultParkingCentavos, state.defaultFuelPriceCentavos) {
        parking = centavosText(state.defaultParkingCentavos)
        fuel = centavosText(state.defaultFuelPriceCentavos)
    }
    LaunchedEffect(state.staleOdometerDays) { staleDays = state.staleOdometerDays.toString() }

    Scaffold(modifier = Modifier.padding(contentPadding)) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) }
            item {
                SettingsSection("Display") {
                    ChoiceRow("Theme", listOf("SYSTEM", "LIGHT", "DARK"), state.theme, actions.setTheme) { it.lowercase().replaceFirstChar(Char::titlecase) }
                    ChoiceRow("Currency", listOf("PHP", "USD", "EUR"), state.currency, actions.setCurrency) { it }
                    Text("Amounts keep their stored numeric value when the currency label changes.", style = MaterialTheme.typography.bodySmall)
                    ChoiceRow("Date format", listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd"), state.dateFormat, actions.setDateFormat) { it }
                    ReadOnlySetting("Distance", "Kilometres (km)", "Mileage is stored in kilometres to keep maintenance calculations consistent.")
                }
            }
            item {
                SettingsSection("Notifications") {
                    SwitchSetting("Allow reminders", "Maintenance, documents, coverage, loan, and odometer alerts", state.notificationsEnabled, actions.setNotificationsEnabled)
                    OutlinedTextField(
                        value = staleDays,
                        onValueChange = { staleDays = it.filter(Char::isDigit) },
                        label = { Text("Remind after no odometer update (days)") },
                        supportingText = { Text("1–365 days") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = { actions.saveReminderDays(staleDays) }, enabled = state.notificationsEnabled) { Text("Save reminder interval") }
                    if (state.motorcycles.isNotEmpty()) {
                        Text("Per motorcycle", fontWeight = FontWeight.SemiBold)
                        state.motorcycles.forEach { bike ->
                            SwitchSetting(
                                bike.name,
                                "Reminders for this motorcycle",
                                bike.id !in state.notificationDisabledMotorcycleIds,
                                { actions.setMotorcycleNotificationsEnabled(bike.id, it) },
                                enabled = state.notificationsEnabled,
                            )
                        }
                    }
                }
            }
            item {
                SettingsSection("Quick-entry defaults") {
                    OutlinedTextField(parking, { parking = decimalOnly(it) }, label = { Text("Parking amount") }, prefix = { Text("${state.currency} ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(fuel, { fuel = decimalOnly(it) }, label = { Text("Fuel price per litre") }, prefix = { Text("${state.currency} ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { actions.saveDefaults(parking, fuel) }) { Text("Save defaults") }
                }
            }
            item {
                SettingsSection("Data") {
                    OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Backup, contentDescription = null)
                        Text(" Backup, restore & export")
                    }
                    Text("Backups stay under your control through Android's document picker.", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.message?.let { message ->
                item {
                    Card(
                        Modifier.fillMaxWidth()
                            .clickable(onClick = actions.clearMessage)
                            .semantics { liveRegion = if (state.isError) LiveRegionMode.Assertive else LiveRegionMode.Polite },
                    ) {
                        Text(message, color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            item {
                SettingsSection("About") {
                    ReadOnlySetting("MotoCare", "Version $versionName", "Offline motorcycle ownership tracker")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun ChoiceRow(title: String, choices: List<String>, selected: String, onSelect: (String) -> Unit, label: (String) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { choice -> FilterChip(selected = selected == choice, onClick = { onSelect(choice) }, label = { Text(label(choice)) }) }
        }
    }
}

@Composable
private fun SwitchSetting(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChecked(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun ReadOnlySetting(title: String, value: String, detail: String) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(value)
        Text(detail, style = MaterialTheme.typography.bodySmall)
    }
}

private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty().ifBlank { "0.1.0" }

private fun centavosText(value: Long): String = "%.2f".format(value / 100.0)
private fun decimalOnly(value: String): String = value.filterIndexed { index, char -> char.isDigit() || (char == '.' && '.' !in value.take(index)) }

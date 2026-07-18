package com.motocare.app.ui.motorcycle

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.ui.components.MotoCareEmptyState
import com.motocare.app.ui.components.MotoCareIconBadge
import java.time.LocalDate
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.toCentavosOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotorcyclesScreen(contentPadding: PaddingValues, viewModel: MotorcyclesViewModel = hiltViewModel()) {
    val motorcycles by viewModel.motorcycles.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MotorcycleEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<MotorcycleEntity?>(null) }
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text("Motorcycles") }) },
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Outlined.Add, "Add motorcycle") } },
    ) { inner ->
        if (motorcycles.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(inner).padding(24.dp), verticalArrangement = Arrangement.Center) {
                MotoCareEmptyState(
                    title = "Your garage is empty",
                    detail = "Add a motorcycle to begin tracking mileage, maintenance, and costs.",
                    icon = Icons.Outlined.TwoWheeler,
                    actionLabel = "Add motorcycle",
                    onAction = { adding = true },
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(motorcycles, key = { it.id }) { bike ->
                    Card(
                        Modifier.fillMaxWidth().clickable { editing = bike },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MotoCareIconBadge(Icons.Outlined.TwoWheeler)
                            Column(Modifier.weight(1f)) {
                                Text(bike.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOf(bike.manufacturer, bike.model, bike.variant).filter { it.isNotBlank() }.joinToString(" "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("${"%,d".format(bike.currentOdometerKm)} km", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                                bike.purchaseDateEpochDay?.let { Text("Purchased ${LocalDate.ofEpochDay(it).asDisplayDate()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            IconButton(onClick = { archiveTarget = bike }) { Icon(Icons.Outlined.Archive, "Archive ${bike.name}") }
                        }
                    }
                }
            }
        }
    }
    if (adding || editing != null) {
        MotorcycleDialog(
            existing = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { viewModel.save(it); adding = false; editing = null },
        )
    }
    archiveTarget?.let { bike ->
        AlertDialog(
            onDismissRequest = { archiveTarget = null },
            title = { Text("Archive ${bike.name}?") },
            text = { Text("Its history stays on this device and can be restored in a future release.") },
            confirmButton = { TextButton(onClick = { viewModel.archive(bike.id); archiveTarget = null }) { Text("Archive") } },
            dismissButton = { TextButton(onClick = { archiveTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MotorcycleDialog(existing: MotorcycleEntity?, onDismiss: () -> Unit, onSave: (MotorcycleEntity) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var maker by remember(existing) { mutableStateOf(existing?.manufacturer.orEmpty()) }
    var model by remember(existing) { mutableStateOf(existing?.model.orEmpty()) }
    var variant by remember(existing) { mutableStateOf(existing?.variant.orEmpty()) }
    var year by remember(existing) { mutableStateOf(existing?.year?.toString().orEmpty()) }
    var purchaseDate by remember(existing) { mutableStateOf(existing?.purchaseDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()) }
    var purchaseType by remember(existing) { mutableStateOf(existing?.purchaseType ?: "UNKNOWN") }
    var purchasePrice by remember(existing) { mutableStateOf(existing?.purchasePriceCentavos?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var seller by remember(existing) { mutableStateOf(existing?.seller.orEmpty()) }
    var secondHand by remember(existing) { mutableStateOf(existing?.secondHand ?: false) }
    var initialKm by remember(existing) { mutableStateOf(existing?.initialOdometerKm?.toString() ?: "1") }
    var plate by remember(existing) { mutableStateOf(existing?.plateNumber.orEmpty()) }
    var engine by remember(existing) { mutableStateOf(existing?.engineNumber.orEmpty()) }
    var chassis by remember(existing) { mutableStateOf(existing?.chassisNumber.orEmpty()) }
    var registration by remember(existing) { mutableStateOf(existing?.registrationExpiryEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()) }
    var insurance by remember(existing) { mutableStateOf(existing?.insuranceExpiryEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()) }
    var financed by remember(existing) { mutableStateOf(existing?.isFinanced ?: false) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var photoUri by remember(existing) { mutableStateOf(existing?.photoUri.orEmpty()) }
    var showSensitive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoUri = uri.toString()
        }
    }
    val valid = name.isNotBlank() && maker.isNotBlank() && model.isNotBlank() && initialKm.toLongOrNull() != null
    fun epochOrNull(value: String): Long? = runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add motorcycle" else "Edit motorcycle") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(name, { name = it }, "Name or nickname")
                Field(maker, { maker = it }, "Manufacturer")
                Field(model, { model = it }, "Model")
                Field(variant, { variant = it }, "Variant")
                Field(year, { year = it.filter(Char::isDigit) }, "Year")
                Field(purchaseDate, { purchaseDate = it }, "Purchase date (YYYY-MM-DD)")
                Text("Purchase type")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH", "FINANCED").forEach { type ->
                        AssistChip(onClick = { purchaseType = type; financed = type == "FINANCED" }, label = { Text(if (purchaseType == type) "✓ ${type.lowercase()}" else type.lowercase()) })
                    }
                }
                Field(purchasePrice, { purchasePrice = it.filter { c -> c.isDigit() || c == '.' } }, "Purchase price (PHP)")
                Field(seller, { seller = it }, "Dealer or seller")
                Row { Checkbox(secondHand, { secondHand = it }); Text("Bought second-hand", Modifier.padding(top = 12.dp)) }
                if (existing == null) Field(initialKm, { initialKm = it.filter(Char::isDigit) }, "Initial odometer (km)")
                Field(plate, { plate = it }, "Plate number")
                Field(registration, { registration = it }, "Registration expiry (YYYY-MM-DD)")
                Field(insurance, { insurance = it }, "Insurance expiry (YYYY-MM-DD)")
                Row { Checkbox(financed, { financed = it }); Text("Financed", Modifier.padding(top = 12.dp)) }
                TextButton(onClick = { showSensitive = !showSensitive }) { Text(if (showSensitive) "Hide sensitive identifiers" else "Show sensitive identifiers") }
                if (showSensitive) {
                    Field(engine, { engine = it }, "Engine number (optional)")
                    Field(chassis, { chassis = it }, "Chassis number (optional)")
                }
                TextButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Text(if (photoUri.isBlank()) "Choose motorcycle photo" else "Change motorcycle photo") }
                if (photoUri.isNotBlank()) Text("Photo selected", style = MaterialTheme.typography.bodySmall)
                Field(notes, { notes = it }, "Notes", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val base = existing ?: MotorcycleEntity(
                        name = "", manufacturer = "", model = "", initialOdometerKm = initialKm.toLongOrNull() ?: 0,
                        currentOdometerKm = initialKm.toLongOrNull() ?: 0,
                    )
                    onSave(base.copy(
                        name = name.trim(), manufacturer = maker.trim(), model = model.trim(), variant = variant.trim(),
                        year = year.toIntOrNull(), purchaseDateEpochDay = epochOrNull(purchaseDate),
                        purchaseType = purchaseType, purchasePriceCentavos = purchasePrice.toCentavosOrNull(), seller = seller.trim(), secondHand = secondHand,
                        initialOdometerKm = if (existing == null) initialKm.toLong() else base.initialOdometerKm,
                        plateNumber = plate.trim(), engineNumber = engine.trim(), chassisNumber = chassis.trim(),
                        registrationExpiryEpochDay = epochOrNull(registration), insuranceExpiryEpochDay = epochOrNull(insurance),
                        isFinanced = financed, notes = notes.trim(), photoUri = photoUri.trim().ifEmpty { null },
                    ))
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth())
}

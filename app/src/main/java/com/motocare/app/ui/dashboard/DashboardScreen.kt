package com.motocare.app.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.domain.model.MaintenanceStatus
import kotlin.math.max

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onAddOdometer: () -> Unit,
    onManageMotorcycles: () -> Unit,
    onMaintenance: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MotoCare", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        val selected = state.selected
        if (selected == null) {
            EmptyDashboard(onManageMotorcycles)
            return@Column
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.motorcycles.forEach { motorcycle ->
                AssistChip(
                    onClick = { viewModel.selectMotorcycle(motorcycle.id) },
                    label = { Text(motorcycle.name) },
                    leadingIcon = if (motorcycle.id == selected.id) ({ Text("✓") }) else null,
                )
            }
        }
        Text("${selected.manufacturer} ${selected.model} ${selected.variant}", style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Text("CURRENT ODOMETER", style = MaterialTheme.typography.labelMedium)
                Text("${"%,d".format(selected.currentOdometerKm)} km", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Avg ${"%.1f".format(state.odometerStats.averageKmPerDay)} km/day • ${"%.0f".format(state.odometerStats.averageKmPerMonth)} km/month")
            }
        }
        Text("Quick actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Odometer", Icons.Outlined.Add, onAddOdometer)
            QuickAction("Maintenance", Icons.Outlined.Build, onMaintenance)
            QuickAction("Fuel", Icons.Outlined.LocalGasStation, {}, enabled = false)
            QuickAction("Parking", Icons.Outlined.LocalParking, {}, enabled = false)
            QuickAction("Expense", Icons.AutoMirrored.Outlined.ReceiptLong, {}, enabled = false)
            QuickAction("Issue", Icons.Outlined.ReportProblem, {}, enabled = false)
        }
        Text("Maintenance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Due soon", state.dueSoonCount.toString(), Modifier.weight(1f))
            MetricCard("Overdue", state.overdueCount.toString(), Modifier.weight(1f), state.overdueCount > 0)
        }
        state.nextSchedule?.let { row ->
            StatusCard(
                title = "Next: ${row.schedule.name}",
                detail = remainingText(row),
                status = row.assessment.status,
            )
        } ?: Text("Set intervals on your editable maintenance templates to see the next service.", style = MaterialTheme.typography.bodyMedium)
        Text("Coverage & ownership", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        state.coverage?.let {
            InfoCard("Free maintenance coverage", "${max(0, it.remainingDays)} days or ${"%,d".format(max(0, it.remainingKm))} km remaining — whichever comes first")
        }
        state.loanSummary?.let { InfoCard("Financing", it) }
        InfoCard("This month", "Fuel —  •  Parking —  •  Total —\nExpense tracking arrives in Phase 2.")
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyDashboard(onManageMotorcycles: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No motorcycle yet", style = MaterialTheme.typography.titleLarge)
            Text("Add a profile to start tracking mileage and maintenance.")
            Button(onClick = onManageMotorcycles) { Text("Add motorcycle") }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = null)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier, alert: Boolean = false) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (alert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label)
        }
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatusCard(title: String, detail: String, status: MaintenanceStatus) {
    val color = when (status) {
        MaintenanceStatus.GOOD -> MaterialTheme.colorScheme.primary
        MaintenanceStatus.DUE_SOON -> Color(0xFF9A6700)
        MaintenanceStatus.DUE -> MaterialTheme.colorScheme.tertiary
        MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.error
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(status.name.replace('_', ' '), color = color, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail)
        }
    }
}

private fun remainingText(row: ScheduleRow): String = listOfNotNull(
    row.assessment.remainingKm?.let { "${"%,d".format(it)} km" },
    row.assessment.remainingDays?.let { "$it days" },
).joinToString(" or ").ifEmpty { "Interval not set" }

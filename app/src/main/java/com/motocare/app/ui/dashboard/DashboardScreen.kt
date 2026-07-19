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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.ui.components.MotoCarePageHeader
import com.motocare.app.ui.components.MotoCareSectionHeader
import com.motocare.app.ui.components.MotoCareStatusPill
import com.motocare.app.ui.components.MotoCareSummaryCard
import com.motocare.app.ui.theme.motoCareStatusColors
import com.motocare.app.util.asDisplayDate
import com.motocare.app.util.asPeso
import kotlin.math.max
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onAddOdometer: () -> Unit,
    onManageMotorcycles: () -> Unit,
    onMaintenance: () -> Unit,
    onAddService: () -> Unit,
    onAddFuel: () -> Unit,
    onAddParking: () -> Unit,
    onAddExpense: () -> Unit,
    onLoan: () -> Unit,
    onIssues: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MotoCarePageHeader(title = "MotoCare", subtitle = "Ready for the road ahead", icon = Icons.Outlined.TwoWheeler)
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
        selected.purchaseDateEpochDay?.let {
            val date = LocalDate.ofEpochDay(it)
            Text("Purchased ${date.asDisplayDate()} • owned ${ChronoUnit.MONTHS.between(date, LocalDate.now()).coerceAtLeast(0)} months")
        }
        MotoCareSummaryCard(
            label = "Current odometer",
            value = "${"%,d".format(selected.currentOdometerKm)} km",
            detail = "Avg ${"%.1f".format(state.odometerStats.averageKmPerDay)} km/day • ${"%.0f".format(state.odometerStats.averageKmPerMonth)} km/month",
            icon = Icons.Outlined.Speed,
            modifier = Modifier.fillMaxWidth(),
        )
        var showAllActions by remember { mutableStateOf(false) }
        val quickLogActions = listOf(
            QuickLogAction(
                label = "Odometer",
                icon = Icons.Outlined.Speed,
                onClick = onAddOdometer,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            QuickLogAction(
                label = "Fuel",
                icon = Icons.Outlined.LocalGasStation,
                onClick = onAddFuel,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            QuickLogAction(
                label = "Service",
                icon = Icons.Outlined.Build,
                onClick = onAddService,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            QuickLogAction(
                label = "Expense",
                icon = Icons.Outlined.Payments,
                onClick = onAddExpense,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        val useSingleColumnActions = LocalDensity.current.fontScale >= 1.5f
        MotoCareSectionHeader("Quick log")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (useSingleColumnActions) {
                quickLogActions.forEach { action ->
                    QuickLogCard(
                        label = action.label,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = action.containerColor,
                        contentColor = action.contentColor,
                    )
                }
            } else {
                quickLogActions.chunked(2).forEach { actions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        actions.forEach { action ->
                            QuickLogCard(
                                label = action.label,
                                icon = action.icon,
                                onClick = action.onClick,
                                modifier = Modifier.weight(1f),
                                containerColor = action.containerColor,
                                contentColor = action.contentColor,
                            )
                        }
                    }
                }
            }
            OutlinedButton(onClick = { showAllActions = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("All actions", Modifier.padding(start = 8.dp))
            }
        }
        if (showAllActions) {
            ModalBottomSheet(onDismissRequest = { showAllActions = false }) {
                AllActionsSheet(
                    onAction = {
                        showAllActions = false
                        it()
                    },
                    onAddOdometer = onAddOdometer,
                    onAddFuel = onAddFuel,
                    onAddService = onAddService,
                    onAddExpense = onAddExpense,
                    onAddParking = onAddParking,
                    onIssues = onIssues,
                    onMaintenance = onMaintenance,
                    onLoan = onLoan,
                )
            }
        }
        MotoCareSectionHeader("Maintenance")
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
        MotoCareSectionHeader("Coverage & ownership")
        state.coverage?.let {
            InfoCard("Free maintenance coverage", "${max(0, it.remainingDays)} days or ${"%,d".format(max(0, it.remainingKm))} km remaining — whichever comes first")
        }
        state.loan?.let {
            InfoCard(
                "Financing",
                "${it.remainingPayments} payments remaining" + (it.nextPaymentDate?.let { date -> " • next ${date.asDisplayDate()}" } ?: "") +
                    "\nPaid ${it.totalPaidCentavos.asPeso()} • rebates ${it.rebatesEarnedCentavos.asPeso()}",
            )
        }
        InfoCard(
            "This month",
            "Fuel ${state.monthFuelCentavos.asPeso()} • Parking ${state.monthParkingCentavos.asPeso()} • Total ${state.cost.monthCentavos.asPeso()}" +
                (state.cost.costPerKmCentavos?.let { "\nOwnership cost ${it.toLong().asPeso()}/km" } ?: ""),
        )
        InfoCard(
            "Ownership costs",
            "Today ${state.cost.todayCentavos.asPeso()} • Year ${state.cost.yearCentavos.asPeso()} • Total ${state.cost.totalCentavos.asPeso()}" +
                (state.cost.costPerKmCentavos?.let { total ->
                    "\nPer km ${total.toLong().asPeso()} • fuel ${state.cost.fuelCostPerKmCentavos?.toLong()?.asPeso() ?: "—"} • maintenance ${state.cost.maintenanceCostPerKmCentavos?.toLong()?.asPeso() ?: "—"}"
                } ?: ""),
        )
        state.fuel.averageKmPerLitre?.let { InfoCard("Fuel economy", "${"%.1f".format(it)} km/L average") }
        state.registration?.expiryEpochDay?.let { InfoCard("Registration", "Expires ${LocalDate.ofEpochDay(it).asDisplayDate()}") }
        state.insurance?.expiryEpochDay?.let { InfoCard("Insurance", "Expires ${LocalDate.ofEpochDay(it).asDisplayDate()}") }
        if (state.unresolvedProblems.isNotEmpty()) {
            InfoCard("Unresolved issues", state.unresolvedProblems.joinToString("\n") { "• ${it.symptom} (${it.severity.lowercase()})" })
        }
        Spacer(Modifier.height(8.dp))
    }
}

private data class QuickLogAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val containerColor: Color,
    val contentColor: Color,
)

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
private fun QuickLogCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(7.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun AllActionsSheet(
    onAction: (() -> Unit) -> Unit,
    onAddOdometer: () -> Unit,
    onAddFuel: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onAddParking: () -> Unit,
    onIssues: () -> Unit,
    onMaintenance: () -> Unit,
    onLoan: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("All actions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Log a record or manage your motorcycle.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text("Log a record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SheetAction("Odometer", "Add a mileage reading", Icons.Outlined.Speed) { onAction(onAddOdometer) }
        SheetAction("Fuel", "Record a fill-up", Icons.Outlined.LocalGasStation) { onAction(onAddFuel) }
        SheetAction("Service", "Add completed maintenance work", Icons.Outlined.Build) { onAction(onAddService) }
        SheetAction("Expense", "Record an ownership cost", Icons.Outlined.Payments) { onAction(onAddExpense) }
        SheetAction("Parking", "Open expenses to add today’s parking", Icons.Outlined.LocalParking) { onAction(onAddParking) }
        SheetAction("Issue", "Report a problem or symptom", Icons.Outlined.ReportProblem) { onAction(onIssues) }
        Text(
            "Manage",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        SheetAction("Maintenance schedules", "Review service intervals", Icons.AutoMirrored.Outlined.ReceiptLong) { onAction(onMaintenance) }
        SheetAction("Financing", "Review loan and payments", Icons.Outlined.Payments) { onAction(onLoan) }
    }
}

@Composable
private fun SheetAction(label: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
    val statusColors = MaterialTheme.motoCareStatusColors
    val (color, container) = when (status) {
        MaintenanceStatus.GOOD -> statusColors.success to statusColors.successContainer
        MaintenanceStatus.DUE_SOON -> statusColors.warning to statusColors.warningContainer
        MaintenanceStatus.DUE -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
        MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MotoCareStatusPill(status.name.replace('_', ' '), color, container)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun remainingText(row: ScheduleRow): String = listOfNotNull(
    row.assessment.remainingKm?.let { "${"%,d".format(it)} km" },
    row.assessment.remainingDays?.let { "$it days" },
).joinToString(" or ").ifEmpty { "Interval not set" }

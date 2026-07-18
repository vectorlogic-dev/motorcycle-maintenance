package com.motocare.app.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TwoWheeler
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
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
        val quickActionScroll = rememberScrollState()
        MotoCareSectionHeader("Quick actions")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(quickActionScroll)
                        .semantics {
                            contentDescription = "Quick actions"
                            stateDescription = when {
                                quickActionScroll.canScrollBackward && quickActionScroll.canScrollForward -> "Middle of list"
                                quickActionScroll.canScrollBackward -> "End of list"
                                quickActionScroll.canScrollForward -> "Start of list; more actions available"
                                else -> "All actions visible"
                            }
                        }
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAction("Odometer", Icons.Outlined.Add, onAddOdometer)
                    QuickAction("Maintenance", Icons.Outlined.Build, onMaintenance)
                    QuickAction("Service", Icons.AutoMirrored.Outlined.ReceiptLong, onAddService)
                    QuickAction("Fuel", Icons.Outlined.LocalGasStation, onAddFuel)
                    QuickAction("Parking", Icons.Outlined.LocalParking, onAddParking)
                    QuickAction("Expense", Icons.AutoMirrored.Outlined.ReceiptLong, onAddExpense)
                    QuickAction("Loan", Icons.Outlined.Payments, onLoan)
                    QuickAction("Issue", Icons.Outlined.ReportProblem, onIssues)
                }
                HorizontalScrollIndicator(quickActionScroll)
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

@Composable
private fun HorizontalScrollIndicator(scrollState: ScrollState) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val thumbColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val horizontalInset = 12.dp.toPx()
        val trackWidth = (size.width - horizontalInset * 2).coerceAtLeast(0f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(horizontalInset, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = CornerRadius(size.height, size.height),
        )
        val contentWidth = size.width + scrollState.maxValue
        val thumbWidth = if (contentWidth > 0) {
            (trackWidth * (size.width / contentWidth)).coerceIn(trackWidth * 0.2f, trackWidth)
        } else trackWidth
        val progress = if (scrollState.maxValue > 0) {
            scrollState.value.toFloat() / scrollState.maxValue
        } else 0f
        val startOffset = (trackWidth - thumbWidth) * progress
        val offset = if (layoutDirection == LayoutDirection.Rtl) {
            trackWidth - thumbWidth - startOffset
        } else startOffset
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(horizontalInset + offset, 0f),
            size = Size(thumbWidth, size.height),
            cornerRadius = CornerRadius(size.height, size.height),
        )
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

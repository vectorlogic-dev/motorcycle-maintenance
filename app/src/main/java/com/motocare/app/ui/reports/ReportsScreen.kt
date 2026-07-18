package com.motocare.app.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motocare.app.util.asPeso
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Reports") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(state.motorcycle?.name ?: "No motorcycle selected", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("This month", state.costSummary.monthCentavos.asPeso(), Modifier.weight(1f))
                SummaryCard("This year", state.costSummary.yearCentavos.asPeso(), Modifier.weight(1f))
            }
            SummaryCard("Ownership cost per km", state.costSummary.costPerKmCentavos?.toLong()?.asPeso()?.plus(" / km") ?: "Not enough mileage", Modifier.fillMaxWidth())
            NativeBarChart("Monthly ownership cost", state.monthlyCosts, { it.asPeso() }, MaterialTheme.colorScheme.primary)
            NativeBarChart("Kilometres travelled", state.monthlyDistance, { "${"%,d".format(it)} km" }, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun NativeBarChart(title: String, points: List<MonthlyPoint>, valueLabel: (Long) -> String, color: Color) {
    val max = points.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val description = points.joinToString(", ") { "${it.month.format(DateTimeFormatter.ofPattern("MMM yyyy"))}: ${valueLabel(it.value)}" }
    val outline = MaterialTheme.colorScheme.outlineVariant
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (points.all { it.value == 0L }) {
                Text(
                    "No data in this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = "$title. $description" },
                )
            } else {
                Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "$title. $description" }) {
                    val slot = size.width / points.size.coerceAtLeast(1)
                    val barWidth = slot * 0.55f
                    drawLine(outline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    points.forEachIndexed { index, point ->
                        val height = size.height * (point.value.toFloat() / max)
                        drawRect(color, Offset(index * slot + (slot - barWidth) / 2, size.height - height), Size(barWidth, height))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    points.forEach { Text(it.month.format(DateTimeFormatter.ofPattern("MMM")), style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

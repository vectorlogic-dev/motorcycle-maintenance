package com.motocare.app.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecordsHubScreen(
    contentPadding: PaddingValues,
    onCoverage: () -> Unit,
    onDocuments: () -> Unit,
    onProblems: () -> Unit,
    onBackup: () -> Unit,
    onReports: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Records & tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        HubCard("Free maintenance coverage", "Track the one-year or kilometre limit", Icons.Outlined.HealthAndSafety, onCoverage)
        HubCard("Registration & insurance", "Store dates, references, and reminders", Icons.Outlined.Description, onDocuments)
        HubCard("Problems & symptoms", "Keep unresolved issues visible", Icons.Outlined.ReportProblem, onProblems)
        HubCard("Reports", "Explore monthly riding and ownership costs", Icons.Outlined.Assessment, onReports)
        HubCard("Backup & export", "JSON backup/restore and CSV exports", Icons.Outlined.Backup, onBackup)
    }
}

@Composable
private fun HubCard(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        androidx.compose.foundation.layout.Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

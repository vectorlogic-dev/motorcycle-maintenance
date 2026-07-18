package com.motocare.app.ui.records

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motocare.app.ui.components.MotoCareLinkCard
import com.motocare.app.ui.components.MotoCarePageHeader

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
        MotoCarePageHeader(title = "Records & tools", subtitle = "Everything important, organised in one place", icon = Icons.Outlined.Description)
        MotoCareLinkCard("Free maintenance coverage", "Track the one-year or kilometre limit", Icons.Outlined.HealthAndSafety, onCoverage)
        MotoCareLinkCard(
            "Registration & insurance",
            "Store dates, references, and reminders",
            Icons.Outlined.Description,
            onDocuments,
            accent = MaterialTheme.colorScheme.tertiary,
            accentContainer = MaterialTheme.colorScheme.tertiaryContainer,
        )
        MotoCareLinkCard(
            "Problems & symptoms",
            "Keep unresolved issues visible",
            Icons.Outlined.ReportProblem,
            onProblems,
            accent = MaterialTheme.colorScheme.error,
            accentContainer = MaterialTheme.colorScheme.errorContainer,
        )
        MotoCareLinkCard("Reports", "Explore monthly riding and ownership costs", Icons.Outlined.Assessment, onReports)
        MotoCareLinkCard(
            "Backup & export",
            "JSON backup/restore and CSV exports",
            Icons.Outlined.Backup,
            onBackup,
            accent = MaterialTheme.colorScheme.secondary,
            accentContainer = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
}

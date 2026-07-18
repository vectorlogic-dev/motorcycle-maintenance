package com.motocare.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("MotoCare", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Maintenance, mileage, and ownership costs — kept on this device.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(40.dp))
        Button(onClick = viewModel::createSample, modifier = Modifier.fillMaxWidth()) {
            Text("Create Honda Click125 sample")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = viewModel::startEmpty, modifier = Modifier.fillMaxWidth()) {
            Text("Start empty")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Sample schedules are editable templates. Confirm intervals with your owner’s manual or dealer booklet.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

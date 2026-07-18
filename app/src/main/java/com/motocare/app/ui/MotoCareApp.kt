package com.motocare.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.ui.dashboard.DashboardScreen
import com.motocare.app.ui.expense.ExpenseScreen
import com.motocare.app.ui.fuel.FuelScreen
import com.motocare.app.ui.loan.LoanScreen
import com.motocare.app.ui.maintenance.MaintenanceScreen
import com.motocare.app.ui.motorcycle.MotorcyclesScreen
import com.motocare.app.ui.odometer.OdometerScreen
import com.motocare.app.ui.onboarding.OnboardingScreen
import com.motocare.app.ui.onboarding.OnboardingViewModel
import com.motocare.app.ui.service.ServiceScreen
import com.motocare.app.ui.records.RecordsHubScreen
import com.motocare.app.ui.records.CoverageScreen
import com.motocare.app.ui.records.DocumentsScreen
import com.motocare.app.ui.problem.ProblemScreen
import com.motocare.app.ui.backup.BackupScreen
import com.motocare.app.ui.reports.ReportsScreen
import com.motocare.app.ui.settings.SettingsScreen
import com.motocare.app.ui.theme.MotoCareTheme
import com.motocare.app.util.DisplayFormats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@HiltViewModel
class AppViewModel @Inject constructor(preferences: PreferencesRepository) : ViewModel() {
    val onboardingComplete = preferences.onboardingComplete.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val theme = preferences.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")
    val currency = preferences.currency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "PHP")
    val dateFormat = preferences.dateFormat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "dd/MM/yyyy")
    val notificationsEnabled = preferences.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(
    Destination("dashboard", "Home", Icons.Outlined.Home),
    Destination("motorcycles", "Motorcycles", Icons.Outlined.TwoWheeler),
    Destination("maintenance", "Maintenance", Icons.Outlined.Build),
    Destination("records", "Records", Icons.Outlined.Folder),
    Destination("settings", "Settings", Icons.Outlined.Settings),
)

@Composable
fun MotoCareApp(appViewModel: AppViewModel = hiltViewModel()) {
    val onboarded by appViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val theme by appViewModel.theme.collectAsStateWithLifecycle()
    val currency by appViewModel.currency.collectAsStateWithLifecycle()
    val dateFormat by appViewModel.dateFormat.collectAsStateWithLifecycle()
    val notificationsEnabled by appViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    DisplayFormats.configure(currency, dateFormat)
    MotoCareTheme(theme) {
        when (onboarded) {
            null -> Unit
            false -> OnboardingScreen(hiltViewModel<OnboardingViewModel>())
            true -> {
                MainNavigation()
                if (notificationsEnabled) NotificationPermissionEffect()
            }
        }
    }
}

@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            if (route in destinations.map { it.route }) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier) {
            composable("dashboard") {
                DashboardScreen(
                    contentPadding = padding,
                    onAddOdometer = { navController.navigate("odometer") },
                    onManageMotorcycles = { navController.navigate("motorcycles") },
                    onMaintenance = { navController.navigate("maintenance") },
                    onAddService = { navController.navigate("services") },
                    onAddFuel = { navController.navigate("fuel") },
                    onAddParking = { navController.navigate("parking") },
                    onAddExpense = { navController.navigate("expenses") },
                    onLoan = { navController.navigate("loan") },
                    onIssues = { navController.navigate("problems") },
                )
            }
            composable("motorcycles") { MotorcyclesScreen(contentPadding = padding) }
            composable("maintenance") { MaintenanceScreen(contentPadding = padding) }
            composable("odometer") { OdometerScreen(onBack = navController::popBackStack) }
            composable("services") { ServiceScreen(onBack = navController::popBackStack) }
            composable("fuel") { FuelScreen(onBack = navController::popBackStack) }
            composable("expenses") { ExpenseScreen(onBack = navController::popBackStack) }
            composable("parking") { ExpenseScreen(onBack = navController::popBackStack, startWithParking = true) }
            composable("loan") { LoanScreen(onBack = navController::popBackStack) }
            composable("records") {
                RecordsHubScreen(
                    contentPadding = padding,
                    onCoverage = { navController.navigate("coverage") },
                    onDocuments = { navController.navigate("documents") },
                    onProblems = { navController.navigate("problems") },
                    onBackup = { navController.navigate("backup") },
                    onReports = { navController.navigate("reports") },
                )
            }
            composable("settings") { SettingsScreen(contentPadding = padding, onBackup = { navController.navigate("backup") }) }
            composable("coverage") { CoverageScreen(onBack = navController::popBackStack) }
            composable("documents") { DocumentsScreen(onBack = navController::popBackStack) }
            composable("problems") { ProblemScreen(onBack = navController::popBackStack) }
            composable("backup") { BackupScreen(onBack = navController::popBackStack) }
            composable("reports") { ReportsScreen(onBack = navController::popBackStack) }
        }
    }
}

package com.motocare.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.motocare.app.ui.records.RecordsHubScreen
import com.motocare.app.ui.settings.SettingsActions
import com.motocare.app.ui.settings.SettingsContent
import com.motocare.app.ui.settings.SettingsUiState
import com.motocare.app.ui.components.MotoCareDateField
import com.motocare.app.ui.components.MotoCareNoMotorcycleState
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.domain.model.MaintenanceAssessment
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.ui.dashboard.ScheduleRow
import com.motocare.app.ui.maintenance.MaintenanceCard
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CriticalScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recordsHub_exposesCriticalToolsAndRoutesReports() {
        var route = ""
        compose.setContent {
            MaterialTheme {
                RecordsHubScreen(
                    contentPadding = PaddingValues(),
                    onCoverage = {},
                    onDocuments = {},
                    onProblems = {},
                    onBackup = {},
                    onReports = { route = "reports" },
                )
            }
        }

        compose.onNodeWithText("Records & tools").assertIsDisplayed()
        compose.onNodeWithText("Reports").performClick()
        assertEquals("reports", route)
        compose.onNodeWithText("Backup & export").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noMotorcycleState_routesToGarage() {
        var openedGarage = false
        compose.setContent {
            MaterialTheme {
                MotoCareNoMotorcycleState(onAddMotorcycle = { openedGarage = true })
            }
        }

        compose.onNodeWithText("No motorcycle yet").assertIsDisplayed()
        compose.onNodeWithText("Add motorcycle").performClick()
        assertEquals(true, openedGarage)
    }

    @Test
    fun maintenanceCard_routesCompletedWorkWithScheduleId() {
        var scheduleId = 0L
        compose.setContent {
            MaterialTheme {
                MaintenanceCard(
                    row = ScheduleRow(
                        schedule = MaintenanceScheduleEntity(
                            id = 27,
                            motorcycleId = 1,
                            name = "Engine oil change",
                            intervalKm = 3_000,
                        ),
                        assessment = MaintenanceAssessment(
                            status = MaintenanceStatus.GOOD,
                            remainingKm = 2_000,
                            remainingDays = null,
                            overdueByDistance = false,
                            overdueByTime = false,
                        ),
                    ),
                    onEdit = {},
                    onDeactivate = {},
                    onRecordService = { scheduleId = 27 },
                )
            }
        }

        compose.onNodeWithText("Record service").performClick()
        assertEquals(27L, scheduleId)
    }

    @Test
    fun settings_exposesPreferencesAndChangesTheme() {
        var theme = ""
        compose.setContent {
            MaterialTheme {
                SettingsContent(
                    state = SettingsUiState(),
                    contentPadding = PaddingValues(),
                    versionName = "test",
                    onBackup = {},
                    actions = SettingsActions(setTheme = { theme = it }),
                )
            }
        }

        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("Dark").performClick()
        assertEquals("DARK", theme)
    }

    @Test
    fun dateField_opensCalendarPicker() {
        compose.setContent {
            MaterialTheme {
                MotoCareDateField(
                    date = LocalDate.of(2026, 7, 16),
                    onDateSelected = {},
                    label = "Reading date",
                )
            }
        }

        compose.onNodeWithContentDescription("Reading date, 16/07/2026. Choose date").performClick()
        compose.onNodeWithText("OK").assertIsDisplayed()
    }
}

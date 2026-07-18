package com.motocare.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.motocare.app.ui.records.RecordsHubScreen
import com.motocare.app.ui.settings.SettingsActions
import com.motocare.app.ui.settings.SettingsContent
import com.motocare.app.ui.settings.SettingsUiState
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
}

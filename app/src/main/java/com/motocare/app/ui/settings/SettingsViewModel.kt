package com.motocare.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currency: String = "PHP",
    val dateFormat: String = "dd/MM/yyyy",
    val theme: String = "SYSTEM",
    val notificationsEnabled: Boolean = false,
    val staleOdometerDays: Int = 14,
    val defaultParkingCentavos: Long = 3_500,
    val defaultFuelPriceCentavos: Long = 7_000,
    val motorcycles: List<MotorcycleEntity> = emptyList(),
    val notificationDisabledMotorcycleIds: Set<Long> = emptySet(),
    val message: String? = null,
    val isError: Boolean = false,
)

private data class GeneralSettings(
    val currency: String,
    val dateFormat: String,
    val theme: String,
    val notificationsEnabled: Boolean,
    val staleDays: Int,
)

private data class DefaultSettings(
    val parking: Long,
    val fuel: Long,
    val disabledIds: Set<Long>,
    val motorcycles: List<MotorcycleEntity>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    motorcycles: MotorcycleRepository,
) : ViewModel() {
    private val feedback = MutableStateFlow<Pair<String, Boolean>?>(null)
    private val general = combine(
        preferences.currency,
        preferences.dateFormat,
        preferences.theme,
        preferences.notificationsEnabled,
        preferences.staleOdometerDays,
    ) { currency, date, theme, notifications, stale ->
        GeneralSettings(currency, date, theme, notifications, stale)
    }
    private val defaults = combine(
        preferences.defaultParkingCentavos,
        preferences.defaultFuelPriceCentavos,
        preferences.notificationDisabledMotorcycleIds,
        motorcycles.activeMotorcycles,
    ) { parking, fuel, disabled, bikes -> DefaultSettings(parking, fuel, disabled, bikes) }

    val uiState = combine(general, defaults, feedback) { general, defaults, message ->
        SettingsUiState(
            currency = general.currency,
            dateFormat = general.dateFormat,
            theme = general.theme,
            notificationsEnabled = general.notificationsEnabled,
            staleOdometerDays = general.staleDays,
            defaultParkingCentavos = defaults.parking,
            defaultFuelPriceCentavos = defaults.fuel,
            motorcycles = defaults.motorcycles,
            notificationDisabledMotorcycleIds = defaults.disabledIds,
            message = message?.first,
            isError = message?.second == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setCurrency(value: String) = update { preferences.setCurrency(value) }
    fun setDateFormat(value: String) = update { preferences.setDateFormat(value) }
    fun setTheme(value: String) = update { preferences.setTheme(value) }
    fun setNotificationsEnabled(value: Boolean) = update { preferences.setNotificationsEnabled(value) }
    fun setMotorcycleNotificationsEnabled(id: Long, value: Boolean) = update {
        preferences.setMotorcycleNotificationsEnabled(id, value)
    }

    fun saveReminderDays(value: String) {
        val days = value.toIntOrNull()
        if (days == null || days !in 1..365) {
            feedback.value = "Enter a reminder interval from 1 to 365 days." to true
            return
        }
        update("Reminder preference saved") { preferences.setStaleOdometerDays(days) }
    }

    fun saveDefaults(parking: String, fuel: String) {
        val parkingCentavos = parking.toCentavos()
        val fuelCentavos = fuel.toCentavos()
        if (parkingCentavos == null || fuelCentavos == null || parkingCentavos < 0 || fuelCentavos < 0) {
            feedback.value = "Enter valid non-negative default amounts." to true
            return
        }
        update("Quick-entry defaults saved") {
            preferences.setDefaultParkingCentavos(parkingCentavos)
            preferences.setDefaultFuelPriceCentavos(fuelCentavos)
        }
    }

    fun clearMessage() { feedback.value = null }

    private fun update(success: String? = null, block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { feedback.value = success?.let { it to false } }
            .onFailure { feedback.value = (it.message ?: "Could not save this setting.") to true }
    }

    private fun String.toCentavos(): Long? = replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()
}

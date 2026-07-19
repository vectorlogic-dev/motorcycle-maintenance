package com.motocare.app.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.usecase.MaintenanceCalculator
import com.motocare.app.ui.dashboard.ScheduleRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MaintenanceUiState(
    val motorcycle: MotorcycleEntity? = null,
    val schedules: List<ScheduleRow> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    private val repository: MaintenanceRepository,
    calculator: MaintenanceCalculator,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id ->
        bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull()
    }
    private val schedules = selected.flatMapLatest { bike -> bike?.let { repository.observeActive(it.id) } ?: flowOf(emptyList()) }
    val uiState = combine(selected, schedules) { bike, plans ->
        MaintenanceUiState(bike, plans.map { ScheduleRow(it, calculator.assess(it, bike?.currentOdometerKm ?: 0)) }, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MaintenanceUiState())

    fun save(input: ScheduleInput) {
        val bike = uiState.value.motorcycle ?: return
        val kmInterval = input.intervalKm.toLongOrNull()?.takeIf { it > 0 }
        val dayInterval = input.intervalDays.toIntOrNull()?.takeIf { it > 0 }
        val lastDate = input.lastServiceDate.takeIf(String::isNotBlank)?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
        val lastKm = input.lastServiceKm.toLongOrNull()
        val base = input.existing ?: MaintenanceScheduleEntity(motorcycleId = bike.id, name = input.name.trim())
        val schedule = base.copy(
            name = input.name.trim(),
            description = input.description.trim(),
            intervalKm = kmInterval,
            intervalDays = dayInterval,
            lastServiceEpochDay = lastDate?.toEpochDay(),
            lastServiceOdometerKm = lastKm,
            nextDueEpochDay = dayInterval?.let { (lastDate ?: LocalDate.now()).plusDays(it.toLong()).toEpochDay() },
            nextDueOdometerKm = kmInterval?.let { (lastKm ?: bike.currentOdometerKm) + it },
            reminderLeadDays = input.leadDays.toIntOrNull() ?: 14,
            reminderLeadKm = input.leadKm.toLongOrNull() ?: 500,
            active = true,
        )
        viewModelScope.launch { if (schedule.id == 0L) repository.add(schedule) else repository.update(schedule) }
    }

    fun deactivate(id: Long) = viewModelScope.launch { repository.deactivate(id) }
}

data class ScheduleInput(
    val existing: MaintenanceScheduleEntity? = null,
    val name: String,
    val description: String,
    val intervalKm: String,
    val intervalDays: String,
    val lastServiceDate: String,
    val lastServiceKm: String,
    val leadDays: String,
    val leadKm: String,
)

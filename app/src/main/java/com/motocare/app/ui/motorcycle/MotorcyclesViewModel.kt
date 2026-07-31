package com.motocare.app.ui.motorcycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.usecase.StarterMaintenanceScheduleFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EquipmentScheduleSuggestion(
    val motorcycleName: String,
    val schedules: List<MaintenanceScheduleEntity>,
)

@HiltViewModel
class MotorcyclesViewModel @Inject constructor(
    private val repository: MotorcycleRepository,
    private val maintenance: MaintenanceRepository,
    private val preferences: PreferencesRepository,
    private val starterSchedules: StarterMaintenanceScheduleFactory,
) : ViewModel() {
    val motorcycles = repository.activeMotorcycles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val archivedMotorcycles = repository.archivedMotorcycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableScheduleSuggestion = MutableStateFlow<EquipmentScheduleSuggestion?>(null)
    val scheduleSuggestion = mutableScheduleSuggestion.asStateFlow()

    fun save(motorcycle: MotorcycleEntity) = viewModelScope.launch {
        if (motorcycle.id == 0L) {
            val id = repository.create(
                motorcycle = motorcycle,
                starterSchedules = starterSchedules.create(
                    motorcycleId = 0,
                    currentOdometerKm = motorcycle.currentOdometerKm,
                    driveType = motorcycle.driveType,
                    coolingType = motorcycle.coolingType,
                ),
            )
            preferences.selectMotorcycle(id)
        } else {
            val previous = repository.get(motorcycle.id)
            repository.update(motorcycle)
            if (previous != null &&
                (previous.driveType != motorcycle.driveType || previous.coolingType != motorcycle.coolingType)
            ) {
                val existingNames = maintenance.getAllForMotorcycle(motorcycle.id)
                    .map { it.name.trim().lowercase() }
                    .toSet()
                val missing = starterSchedules.create(
                    motorcycleId = motorcycle.id,
                    currentOdometerKm = motorcycle.currentOdometerKm,
                    driveType = motorcycle.driveType,
                    coolingType = motorcycle.coolingType,
                ).filterNot { it.name.trim().lowercase() in existingNames }
                if (missing.isNotEmpty()) {
                    mutableScheduleSuggestion.value = EquipmentScheduleSuggestion(motorcycle.name, missing)
                }
            }
        }
    }

    fun addSuggestedSchedules() = viewModelScope.launch {
        mutableScheduleSuggestion.value?.let { maintenance.addAll(it.schedules) }
        mutableScheduleSuggestion.value = null
    }

    fun dismissSuggestedSchedules() {
        mutableScheduleSuggestion.value = null
    }

    fun archive(id: Long) = viewModelScope.launch { repository.archive(id) }
    fun restore(id: Long) = viewModelScope.launch { repository.restore(id) }
    fun deletePermanently(id: Long) = viewModelScope.launch { repository.deletePermanently(id) }
}

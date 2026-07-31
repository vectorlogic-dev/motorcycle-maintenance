package com.motocare.app.ui.motorcycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.usecase.StarterMaintenanceScheduleFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MotorcyclesViewModel @Inject constructor(
    private val repository: MotorcycleRepository,
    private val odometers: OdometerRepository,
    private val maintenance: MaintenanceRepository,
    private val preferences: PreferencesRepository,
    private val starterSchedules: StarterMaintenanceScheduleFactory,
) : ViewModel() {
    val motorcycles = repository.activeMotorcycles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(motorcycle: MotorcycleEntity) = viewModelScope.launch {
        if (motorcycle.id == 0L) {
            val id = repository.add(motorcycle)
            odometers.addReading(id, motorcycle.initialOdometerKm, System.currentTimeMillis(), "Starting odometer", false)
            maintenance.addAll(
                starterSchedules.create(
                    motorcycleId = id,
                    currentOdometerKm = motorcycle.currentOdometerKm,
                    driveType = motorcycle.driveType,
                    coolingType = motorcycle.coolingType,
                ),
            )
            preferences.selectMotorcycle(id)
        } else repository.update(motorcycle)
    }

    fun archive(id: Long) = viewModelScope.launch { repository.archive(id) }
}

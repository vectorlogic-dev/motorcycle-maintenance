package com.motocare.app.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.data.repository.ServiceRepository
import com.motocare.app.util.toCentavosOrNull
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

data class ServiceUiState(
    val motorcycle: MotorcycleEntity? = null,
    val schedules: List<MaintenanceScheduleEntity> = emptyList(),
    val records: List<ServiceRecordEntity> = emptyList(),
)

data class ServiceInput(
    val date: String,
    val odometerKm: String,
    val scheduleIds: Set<Long>,
    val mechanic: String,
    val labourCost: String,
    val partsCost: String,
    val partsReplaced: String,
    val notes: String,
    val receiptUris: List<String>,
    val nextDate: String,
    val nextOdometerKm: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ServiceViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    maintenance: MaintenanceRepository,
    private val repository: ServiceRepository,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id ->
        bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull()
    }
    private val schedules = selected.flatMapLatest { it?.let { bike -> maintenance.observeActive(bike.id) } ?: flowOf(emptyList()) }
    private val records = selected.flatMapLatest { it?.let { bike -> repository.observe(bike.id) } ?: flowOf(emptyList()) }
    val uiState = combine(selected, schedules, records) { bike, plans, history -> ServiceUiState(bike, plans, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceUiState())

    fun add(input: ServiceInput, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        val date = runCatching { LocalDate.parse(input.date) }.getOrNull() ?: return
        val odometer = input.odometerKm.toLongOrNull() ?: return
        val record = ServiceRecordEntity(
            motorcycleId = bike.id,
            serviceEpochDay = date.toEpochDay(),
            odometerKm = odometer,
            dealerOrMechanic = input.mechanic.trim(),
            labourCostCentavos = input.labourCost.toCentavosOrNull() ?: 0,
            partsCostCentavos = input.partsCost.toCentavosOrNull() ?: 0,
            partsReplaced = input.partsReplaced.trim(),
            notes = input.notes.trim(),
            nextRecommendedEpochDay = input.nextDate.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() },
            nextRecommendedOdometerKm = input.nextOdometerKm.toLongOrNull(),
        )
        viewModelScope.launch {
            repository.add(record, input.scheduleIds, input.receiptUris)
            onSaved()
        }
    }
}

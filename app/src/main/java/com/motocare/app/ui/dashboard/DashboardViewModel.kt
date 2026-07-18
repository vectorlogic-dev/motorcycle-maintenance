package com.motocare.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.dao.PhaseTwoDao
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.model.CoverageAssessment
import com.motocare.app.domain.model.MaintenanceAssessment
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.domain.model.OdometerStats
import com.motocare.app.domain.usecase.CoverageCalculator
import com.motocare.app.domain.usecase.MaintenanceCalculator
import com.motocare.app.domain.usecase.OdometerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleRow(val schedule: MaintenanceScheduleEntity, val assessment: MaintenanceAssessment)

data class DashboardUiState(
    val motorcycles: List<MotorcycleEntity> = emptyList(),
    val selected: MotorcycleEntity? = null,
    val schedules: List<ScheduleRow> = emptyList(),
    val odometerStats: OdometerStats = OdometerStats(),
    val coverage: CoverageAssessment? = null,
    val loanSummary: String? = null,
) {
    val dueSoonCount: Int get() = schedules.count { it.assessment.status == MaintenanceStatus.DUE_SOON }
    val overdueCount: Int get() = schedules.count { it.assessment.status == MaintenanceStatus.OVERDUE }
    val nextSchedule: ScheduleRow? get() = schedules
        .filter { it.schedule.nextDueEpochDay != null || it.schedule.nextDueOdometerKm != null }
        .minByOrNull { minOf(it.assessment.remainingKm ?: Long.MAX_VALUE, it.assessment.remainingDays ?: Long.MAX_VALUE) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    odometers: OdometerRepository,
    maintenance: MaintenanceRepository,
    private val preferences: PreferencesRepository,
    phaseTwoDao: PhaseTwoDao,
    maintenanceCalculator: MaintenanceCalculator,
    odometerCalculator: OdometerCalculator,
    coverageCalculator: CoverageCalculator,
) : ViewModel() {
    private val selection = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, preferred ->
        bikes to (bikes.firstOrNull { it.id == preferred } ?: bikes.firstOrNull())
    }
    private val selectedId = selection.flatMapLatest { (_, selected) -> flowOf(selected?.id) }
    private val schedules = selectedId.flatMapLatest { id -> id?.let(maintenance::observeActive) ?: flowOf(emptyList()) }
    private val readings = selectedId.flatMapLatest { id -> id?.let(odometers::observe) ?: flowOf(emptyList()) }
    private val coverage = selectedId.flatMapLatest { id -> id?.let(phaseTwoDao::observeCoverage) ?: flowOf(null) }
    private val loan = selectedId.flatMapLatest { id -> id?.let(phaseTwoDao::observeLoan) ?: flowOf(null) }

    private val base = combine(selection, schedules, readings) { (bikes, selected), plans, entries ->
        val stats = odometerCalculator.stats(entries)
        Triple(
            DashboardUiState(
                motorcycles = bikes,
                selected = selected,
                schedules = plans.map { ScheduleRow(it, maintenanceCalculator.assess(it, selected?.currentOdometerKm ?: 0)) },
                odometerStats = stats,
            ),
            selected,
            stats,
        )
    }

    val uiState = combine(base, coverage, loan) { (state, selected, stats), plan, finance ->
        state.copy(
            coverage = if (plan != null && selected != null) {
                coverageCalculator.assess(plan, selected.currentOdometerKm, stats.averageKmPerDay)
            } else null,
            loanSummary = finance?.let { "${it.termMonths} payments • ₱${"%,.0f".format(it.monthlyPaymentCentavos / 100.0)}/month" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectMotorcycle(id: Long) = viewModelScope.launch { preferences.selectMotorcycle(id) }
}

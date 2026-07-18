package com.motocare.app.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.data.local.entity.InsuranceRecordEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.RegistrationRecordEntity
import com.motocare.app.data.repository.ComplianceRepository
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.model.CoverageAssessment
import com.motocare.app.domain.usecase.CoverageCalculator
import com.motocare.app.domain.usecase.OdometerCalculator
import com.motocare.app.ui.statsFor
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

data class ComplianceUiState(
    val motorcycle: MotorcycleEntity? = null,
    val coverage: CoveragePlanEntity? = null,
    val coverageAssessment: CoverageAssessment? = null,
    val upcomingCoveredSchedules: List<MaintenanceScheduleEntity> = emptyList(),
    val registration: RegistrationRecordEntity? = null,
    val insurance: InsuranceRecordEntity? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComplianceViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    odometers: OdometerRepository,
    maintenance: MaintenanceRepository,
    private val repository: ComplianceRepository,
    coverageCalculator: CoverageCalculator,
    odometerCalculator: OdometerCalculator,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id -> bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull() }
    private val selectedId = selected.flatMapLatest { flowOf(it?.id) }
    private val coverage = selectedId.flatMapLatest { it?.let(repository::observeCoverage) ?: flowOf(null) }
    private val registration = selectedId.flatMapLatest { it?.let(repository::observeRegistration) ?: flowOf(null) }
    private val insurance = selectedId.flatMapLatest { it?.let(repository::observeInsurance) ?: flowOf(null) }
    private val readings = selectedId.flatMapLatest { it?.let(odometers::observe) ?: flowOf(emptyList()) }
    private val schedules = selectedId.flatMapLatest { it?.let(maintenance::observeActive) ?: flowOf(emptyList()) }
    private val base = combine(selected, coverage, readings, schedules) { bike, plan, entries, plans ->
        val assessment = if (bike != null && plan != null) {
            val stats = odometerCalculator.statsFor(bike, entries)
            coverageCalculator.assess(plan, bike.currentOdometerKm, stats.averageKmPerDay)
        } else null
        val upcoming = if (plan == null) emptyList() else plans.filter { schedule ->
            (schedule.nextDueEpochDay?.let { it <= plan.endEpochDay } == true) ||
                (schedule.nextDueOdometerKm?.let { it <= plan.limitOdometerKm } == true)
        }
        Triple(bike, assessment, upcoming)
    }
    val uiState = combine(base, coverage, registration, insurance) { (bike, assessment, upcoming), plan, registrationRecord, insuranceRecord ->
        ComplianceUiState(bike, plan, assessment, upcoming, registrationRecord, insuranceRecord)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComplianceUiState())

    fun saveCoverage(input: CoverageInput, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        val start = runCatching { LocalDate.parse(input.startDate) }.getOrNull() ?: return
        val end = runCatching { LocalDate.parse(input.endDate) }.getOrNull() ?: return
        val plan = (uiState.value.coverage ?: CoveragePlanEntity(
            motorcycleId = bike.id, startEpochDay = start.toEpochDay(), endEpochDay = end.toEpochDay(),
            startOdometerKm = bike.initialOdometerKm, limitOdometerKm = 12_000,
        )).copy(
            startEpochDay = start.toEpochDay(), endEpochDay = end.toEpochDay(),
            startOdometerKm = input.startKm.toLongOrNull() ?: bike.initialOdometerKm,
            limitOdometerKm = input.limitKm.toLongOrNull() ?: 12_000,
            coveredServices = input.services.trim(), coveredLabour = input.labour, coveredParts = input.parts,
            notes = input.notes.trim(), dealerName = input.dealer.trim(),
        )
        viewModelScope.launch { repository.saveCoverage(plan); onSaved() }
    }

    fun saveRegistration(input: RegistrationInput, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        fun epoch(value: String) = value.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
        val record = (uiState.value.registration ?: RegistrationRecordEntity(motorcycleId = bike.id)).copy(
            orDateEpochDay = epoch(input.orDate), crDateEpochDay = epoch(input.crDate), expiryEpochDay = epoch(input.expiry),
            plateNumber = input.plate.trim(), temporaryPlate = input.temporaryPlate,
            dealerSubmissionEpochDay = epoch(input.submissionDate), ltoTransactionReference = input.reference.trim(), notes = input.notes.trim(),
        )
        viewModelScope.launch { repository.saveRegistration(record); onSaved() }
    }

    fun saveInsurance(input: InsuranceInput, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        fun epoch(value: String) = value.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
        val record = (uiState.value.insurance ?: InsuranceRecordEntity(motorcycleId = bike.id)).copy(
            provider = input.provider.trim(), policyNumber = input.policy.trim(), startEpochDay = epoch(input.startDate),
            expiryEpochDay = epoch(input.expiry), notes = input.notes.trim(),
        )
        viewModelScope.launch { repository.saveInsurance(record); onSaved() }
    }
}

data class CoverageInput(val startDate: String, val endDate: String, val startKm: String, val limitKm: String, val services: String, val labour: Boolean, val parts: Boolean, val dealer: String, val notes: String)
data class RegistrationInput(val orDate: String, val crDate: String, val expiry: String, val plate: String, val temporaryPlate: Boolean, val submissionDate: String, val reference: String, val notes: String)
data class InsuranceInput(val provider: String, val policy: String, val startDate: String, val expiry: String, val notes: String)

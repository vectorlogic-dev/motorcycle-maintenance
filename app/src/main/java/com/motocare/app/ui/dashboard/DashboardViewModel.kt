package com.motocare.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.dao.PhaseTwoDao
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.ExpenseRepository
import com.motocare.app.data.repository.ComplianceRepository
import com.motocare.app.data.repository.FuelRepository
import com.motocare.app.data.repository.LoanRepository
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.data.repository.ProblemRepository
import com.motocare.app.data.repository.ServiceRepository
import com.motocare.app.domain.model.CostSummary
import com.motocare.app.domain.model.CoverageAssessment
import com.motocare.app.domain.model.FuelSummary
import com.motocare.app.domain.model.LoanSummary
import com.motocare.app.domain.model.MaintenanceAssessment
import com.motocare.app.domain.model.MaintenanceStatus
import com.motocare.app.domain.model.OdometerStats
import com.motocare.app.data.local.entity.ProblemLogEntity
import com.motocare.app.data.local.entity.RegistrationRecordEntity
import com.motocare.app.data.local.entity.InsuranceRecordEntity
import com.motocare.app.domain.usecase.CostSummaryCalculator
import com.motocare.app.domain.usecase.CoverageCalculator
import com.motocare.app.domain.usecase.FuelEconomyCalculator
import com.motocare.app.domain.usecase.LoanCalculator
import com.motocare.app.domain.usecase.MaintenanceCalculator
import com.motocare.app.domain.usecase.OdometerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ScheduleRow(val schedule: MaintenanceScheduleEntity, val assessment: MaintenanceAssessment)

data class DashboardUiState(
    val motorcycles: List<MotorcycleEntity> = emptyList(),
    val selected: MotorcycleEntity? = null,
    val schedules: List<ScheduleRow> = emptyList(),
    val odometerStats: OdometerStats = OdometerStats(),
    val coverage: CoverageAssessment? = null,
    val loan: LoanSummary? = null,
    val cost: CostSummary = CostSummary(0, 0, 0, 0, null, null, null),
    val fuel: FuelSummary = FuelSummary(),
    val monthFuelCentavos: Long = 0,
    val monthParkingCentavos: Long = 0,
    val registration: RegistrationRecordEntity? = null,
    val insurance: InsuranceRecordEntity? = null,
    val unresolvedProblems: List<ProblemLogEntity> = emptyList(),
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
    expensesRepository: ExpenseRepository,
    fuelRepository: FuelRepository,
    serviceRepository: ServiceRepository,
    complianceRepository: ComplianceRepository,
    problemRepository: ProblemRepository,
    loanRepository: LoanRepository,
    private val preferences: PreferencesRepository,
    phaseTwoDao: PhaseTwoDao,
    maintenanceCalculator: MaintenanceCalculator,
    odometerCalculator: OdometerCalculator,
    coverageCalculator: CoverageCalculator,
    costCalculator: CostSummaryCalculator,
    fuelCalculator: FuelEconomyCalculator,
    loanCalculator: LoanCalculator,
) : ViewModel() {
    private val selection = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, preferred ->
        bikes to (bikes.firstOrNull { it.id == preferred } ?: bikes.firstOrNull())
    }
    private val selectedId = selection.flatMapLatest { (_, selected) -> flowOf(selected?.id) }
    private val schedules = selectedId.flatMapLatest { id -> id?.let(maintenance::observeActive) ?: flowOf(emptyList()) }
    private val readings = selectedId.flatMapLatest { id -> id?.let(odometers::observe) ?: flowOf(emptyList()) }
    private val coverage = selectedId.flatMapLatest { id -> id?.let(phaseTwoDao::observeCoverage) ?: flowOf(null) }
    private val loan = selectedId.flatMapLatest { id -> id?.let(phaseTwoDao::observeLoan) ?: flowOf(null) }
    private val payments = selectedId.flatMapLatest { id -> id?.let(loanRepository::observePayments) ?: flowOf(emptyList()) }
    private val expenses = selectedId.flatMapLatest { id -> id?.let(expensesRepository::observe) ?: flowOf(emptyList()) }
    private val fuelEntries = selectedId.flatMapLatest { id -> id?.let(fuelRepository::observe) ?: flowOf(emptyList()) }
    private val services = selectedId.flatMapLatest { id -> id?.let(serviceRepository::observe) ?: flowOf(emptyList()) }
    private val registration = selectedId.flatMapLatest { id -> id?.let(complianceRepository::observeRegistration) ?: flowOf(null) }
    private val insurance = selectedId.flatMapLatest { id -> id?.let(complianceRepository::observeInsurance) ?: flowOf(null) }
    private val problems = selectedId.flatMapLatest { id -> id?.let(problemRepository::observe) ?: flowOf(emptyList()) }

    private val base = combine(selection, schedules, readings) { (bikes, selected), plans, entries ->
        val stats = odometerCalculator.stats(
            entries = entries,
            initialReadingKm = selected?.initialOdometerKm,
            initialDate = selected?.purchaseDateEpochDay?.let(LocalDate::ofEpochDay),
        )
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
    private val ownershipActivity = combine(expenses, fuelEntries, services) { costs, fills, history -> Triple(costs, fills, history) }
    private val finance = combine(loan, payments) { agreement, installments -> agreement to installments }
    private val phaseThree = combine(registration, insurance, problems) { registrationRecord, insuranceRecord, issues -> Triple(registrationRecord, insuranceRecord, issues) }

    val uiState = combine(base, coverage, ownershipActivity, finance, phaseThree) { (state, selected, stats), plan, activity, financeData, records ->
        val (costs, fills, history) = activity
        val (agreement, installments) = financeData
        val (registrationRecord, insuranceRecord, issues) = records
        val loanSummary = agreement?.let { loanCalculator.calculate(it, installments) }
        val month = YearMonth.now()
        val today = LocalDate.now()
        val paidInstallments = installments.filter { it.status == "PAID_ON_TIME" || it.status == "PAID_LATE" }
        val downPaymentDate = agreement?.startEpochDay?.let(LocalDate::ofEpochDay)
        val loanToday = paidInstallments.filter { it.paidEpochDay == today.toEpochDay() }.sumOf { it.amountCentavos } +
            if (downPaymentDate == today) agreement?.downPaymentCentavos ?: 0 else 0
        val loanMonth = paidInstallments.filter { it.paidEpochDay?.let(LocalDate::ofEpochDay)?.let(YearMonth::from) == month }.sumOf { it.amountCentavos } +
            if (downPaymentDate?.let(YearMonth::from) == month) agreement?.downPaymentCentavos ?: 0 else 0
        val loanYear = paidInstallments.filter { it.paidEpochDay?.let(LocalDate::ofEpochDay)?.year == today.year }.sumOf { it.amountCentavos } +
            if (downPaymentDate?.year == today.year) agreement?.downPaymentCentavos ?: 0 else 0
        val baseCost = costCalculator.calculate(costs, fills, history, stats.travelledKm, loanSummary?.totalPaidCentavos ?: 0)
        state.copy(
            coverage = if (plan != null && selected != null) coverageCalculator.assess(plan, selected.currentOdometerKm, stats.averageKmPerDay) else null,
            loan = loanSummary,
            cost = baseCost.copy(
                todayCentavos = baseCost.todayCentavos + loanToday,
                monthCentavos = baseCost.monthCentavos + loanMonth,
                yearCentavos = baseCost.yearCentavos + loanYear,
            ),
            fuel = fuelCalculator.calculate(fills),
            monthFuelCentavos = fills.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.totalCostCentavos },
            monthParkingCentavos = costs.filter { it.category == "PARKING" && YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.amountCentavos },
            registration = registrationRecord ?: selected?.registrationExpiryEpochDay?.let { RegistrationRecordEntity(motorcycleId = selected.id, expiryEpochDay = it, plateNumber = selected.plateNumber) },
            insurance = insuranceRecord ?: selected?.insuranceExpiryEpochDay?.let { InsuranceRecordEntity(motorcycleId = selected.id, expiryEpochDay = it) },
            unresolvedProblems = issues.filterNot { it.resolved },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectMotorcycle(id: Long) = viewModelScope.launch { preferences.selectMotorcycle(id) }
}

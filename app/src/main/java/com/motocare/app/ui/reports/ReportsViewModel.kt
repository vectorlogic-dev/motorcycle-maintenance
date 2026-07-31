package com.motocare.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.ExpenseRepository
import com.motocare.app.data.repository.FuelRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.data.repository.ServiceRepository
import com.motocare.app.data.repository.LoanRepository
import com.motocare.app.domain.model.CostSummary
import com.motocare.app.domain.usecase.CostSummaryCalculator
import com.motocare.app.domain.usecase.OdometerCalculator
import com.motocare.app.domain.usecase.OwnershipCostTimeline
import com.motocare.app.ui.statsFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MonthlyPoint(val month: YearMonth, val value: Long)
data class ReportsUiState(
    val motorcycle: MotorcycleEntity? = null,
    val monthlyCosts: List<MonthlyPoint> = emptyList(),
    val monthlyDistance: List<MonthlyPoint> = emptyList(),
    val costSummary: CostSummary = CostSummary(0, 0, 0, 0, null, null, null),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    expenses: ExpenseRepository,
    fuel: FuelRepository,
    services: ServiceRepository,
    loans: LoanRepository,
    odometers: OdometerRepository,
    odometerCalculator: OdometerCalculator,
    costCalculator: CostSummaryCalculator,
    ownershipCostTimeline: OwnershipCostTimeline,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id -> bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull() }
    private val selectedId = selected.flatMapLatest { flowOf(it?.id) }
    private val expenseFlow = selectedId.flatMapLatest { it?.let(expenses::observe) ?: flowOf(emptyList()) }
    private val fuelFlow = selectedId.flatMapLatest { it?.let(fuel::observe) ?: flowOf(emptyList()) }
    private val serviceFlow = selectedId.flatMapLatest { it?.let(services::observe) ?: flowOf(emptyList()) }
    private val odometerFlow = selectedId.flatMapLatest { it?.let(odometers::observe) ?: flowOf(emptyList()) }
    private val loanFlow = selectedId.flatMapLatest { it?.let(loans::observeLoan) ?: flowOf(null) }
    private val paymentFlow = selectedId.flatMapLatest { it?.let(loans::observePayments) ?: flowOf(emptyList()) }
    private val activity = combine(expenseFlow, fuelFlow, serviceFlow) { costs, fills, history ->
        Triple(costs, fills, history)
    }
    private val mobilityAndFinance = combine(odometerFlow, loanFlow, paymentFlow) { readings, loan, payments ->
        Triple(readings, loan, payments)
    }

    val uiState = combine(selected, activity, mobilityAndFinance) { bike, activityData, financeData ->
        val (costs, fills, history) = activityData
        val (readings, loan, payments) = financeData
        val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
        val stats = odometerCalculator.statsFor(bike, readings)
        val ownershipCosts = ownershipCostTimeline.build(bike, loan, payments)
        val monthlyCost = months.map { month ->
            val total = costs.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.amountCentavos } +
                fills.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.totalCostCentavos } +
                history.filter { YearMonth.from(LocalDate.ofEpochDay(it.serviceEpochDay)) == month }.sumOf { it.labourCostCentavos + it.partsCostCentavos } +
                ownershipCosts.filter { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == month }.sumOf { it.amountCentavos }
            MonthlyPoint(month, total)
        }
        ReportsUiState(
            motorcycle = bike,
            monthlyCosts = monthlyCost,
            monthlyDistance = months.map { MonthlyPoint(it, stats.travelledByMonth[it.toString()] ?: 0) },
            costSummary = costCalculator.calculate(costs, fills, history, stats.travelledKm, ownershipCosts),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}

package com.motocare.app.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.LoanRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.model.LoanSummary
import com.motocare.app.domain.usecase.LoanCalculator
import com.motocare.app.util.toCentavosOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LoanUiState(
    val motorcycle: MotorcycleEntity? = null,
    val loan: LoanEntity? = null,
    val payments: List<LoanPaymentEntity> = emptyList(),
    val summary: LoanSummary? = null,
    val isLoading: Boolean = true,
)

data class LoanInput(
    val cashPrice: String,
    val downPayment: String,
    val monthlyPayment: String,
    val termMonths: String,
    val dueDay: String,
    val rebate: String,
    val startDate: String,
    val notes: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    private val repository: LoanRepository,
    calculator: LoanCalculator,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id ->
        bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull()
    }
    private val loan = selected.flatMapLatest { it?.let { bike -> repository.observeLoan(bike.id) } ?: flowOf(null) }
    private val payments = selected.flatMapLatest { it?.let { bike -> repository.observePayments(bike.id) } ?: flowOf(emptyList()) }
    val uiState = combine(selected, loan, payments) { bike, finance, installments ->
        LoanUiState(bike, finance, installments, finance?.let { calculator.calculate(it, installments) }, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanUiState())

    init {
        viewModelScope.launch { selected.filterNotNull().collect { repository.ensurePayments(it.id) } }
    }

    fun configure(input: LoanInput, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        val monthly = input.monthlyPayment.toCentavosOrNull() ?: return
        val term = input.termMonths.toIntOrNull()?.takeIf { it > 0 } ?: return
        val start = runCatching { LocalDate.parse(input.startDate) }.getOrNull() ?: return
        viewModelScope.launch {
            repository.configure(
                LoanEntity(
                    motorcycleId = bike.id,
                    cashPriceCentavos = input.cashPrice.toCentavosOrNull(),
                    downPaymentCentavos = input.downPayment.toCentavosOrNull() ?: 0,
                    monthlyPaymentCentavos = monthly,
                    termMonths = term,
                    paymentDueDay = input.dueDay.toIntOrNull()?.coerceIn(1, 31),
                    rebateCentavos = input.rebate.toCentavosOrNull() ?: 0,
                    startEpochDay = start.toEpochDay(),
                    notes = input.notes.trim(),
                ),
            )
            onSaved()
        }
    }

    fun mark(payment: LoanPaymentEntity, status: String) {
        val loan = uiState.value.loan ?: return
        viewModelScope.launch { repository.markPayment(payment, status, loan) }
    }
}

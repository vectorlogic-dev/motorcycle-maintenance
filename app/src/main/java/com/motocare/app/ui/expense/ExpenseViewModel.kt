package com.motocare.app.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.ExpenseEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.ExpenseRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
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
import java.time.YearMonth
import javax.inject.Inject

data class ExpenseUiState(
    val motorcycle: MotorcycleEntity? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val defaultParkingCentavos: Long = 3_500,
) {
    val todayTotalCentavos: Long get() = expenses.filter { it.dateEpochDay == LocalDate.now().toEpochDay() }.sumOf { it.amountCentavos }
    val monthTotalCentavos: Long get() {
        val month = YearMonth.now()
        return expenses.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }.sumOf { it.amountCentavos }
    }
    val yearTotalCentavos: Long get() = expenses.filter { LocalDate.ofEpochDay(it.dateEpochDay).year == LocalDate.now().year }.sumOf { it.amountCentavos }
}

data class ExpenseInput(
    val date: String,
    val category: String,
    val amount: String,
    val odometerKm: String,
    val description: String,
    val receiptUri: String?,
    val paymentMethod: String,
    val vendor: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    private val preferences: PreferencesRepository,
    private val repository: ExpenseRepository,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id ->
        bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull()
    }
    private val expenses = selected.flatMapLatest { it?.let { bike -> repository.observe(bike.id) } ?: flowOf(emptyList()) }
    val uiState = combine(selected, expenses, preferences.defaultParkingCentavos) { bike, costs, parking ->
        ExpenseUiState(bike, costs, parking)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())

    fun addParking() = viewModelScope.launch {
        uiState.value.motorcycle?.let { repository.addParking(it.id, uiState.value.defaultParkingCentavos) }
    }

    fun setParkingDefault(amountText: String) {
        val amount = amountText.toCentavosOrNull() ?: return
        viewModelScope.launch { preferences.setDefaultParkingCentavos(amount) }
    }

    fun add(input: ExpenseInput, onSaved: () -> Unit) = save(input, null, onSaved)

    fun update(existing: ExpenseEntity, input: ExpenseInput, onSaved: () -> Unit) = save(input, existing, onSaved)

    private fun save(input: ExpenseInput, existing: ExpenseEntity?, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        val date = runCatching { LocalDate.parse(input.date) }.getOrNull() ?: return
        val amount = input.amount.toCentavosOrNull() ?: return
        viewModelScope.launch {
            val expense = (existing ?: ExpenseEntity(
                    motorcycleId = bike.id,
                    dateEpochDay = date.toEpochDay(), category = input.category, amountCentavos = amount,
                )).copy(
                    dateEpochDay = date.toEpochDay(),
                    category = input.category,
                    amountCentavos = amount,
                    odometerKm = input.odometerKm.toLongOrNull(),
                    description = input.description.trim(),
                    receiptUri = input.receiptUri,
                    paymentMethod = input.paymentMethod.trim(),
                    vendor = input.vendor.trim(),
                )
            if (existing == null) repository.add(expense) else repository.update(expense)
            onSaved()
        }
    }

    fun delete(expense: ExpenseEntity) = viewModelScope.launch { repository.delete(expense) }
}

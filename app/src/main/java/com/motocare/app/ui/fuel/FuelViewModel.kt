package com.motocare.app.ui.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.FuelRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.model.FuelSummary
import com.motocare.app.domain.usecase.FuelEconomyCalculator
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

data class FuelUiState(
    val motorcycle: MotorcycleEntity? = null,
    val entries: List<FuelEntryEntity> = emptyList(),
    val summary: FuelSummary = FuelSummary(),
    val defaultPriceCentavos: Long = 7_000,
)

data class FuelInput(
    val date: String,
    val odometerKm: String,
    val litres: String,
    val pricePerLitre: String,
    val fullTank: Boolean,
    val station: String,
    val notes: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FuelViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    private val repository: FuelRepository,
    calculator: FuelEconomyCalculator,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id ->
        bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull()
    }
    private val entries = selected.flatMapLatest { it?.let { bike -> repository.observe(bike.id) } ?: flowOf(emptyList()) }
    val uiState = combine(selected, entries, preferences.defaultFuelPriceCentavos) { bike, fills, price ->
        FuelUiState(bike, fills, calculator.calculate(fills), price)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelUiState())

    fun add(input: FuelInput, onSaved: () -> Unit) = save(input, null, onSaved)

    fun update(existing: FuelEntryEntity, input: FuelInput, onSaved: () -> Unit) = save(input, existing, onSaved)

    private fun save(input: FuelInput, existing: FuelEntryEntity?, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        val date = runCatching { LocalDate.parse(input.date) }.getOrNull() ?: return
        val odometer = input.odometerKm.toLongOrNull() ?: return
        val litres = input.litres.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        val price = input.pricePerLitre.toCentavosOrNull() ?: return
        viewModelScope.launch {
            repository.save(
                (existing ?: FuelEntryEntity(
                    motorcycleId = bike.id,
                    dateEpochDay = date.toEpochDay(), odometerKm = odometer, litres = litres,
                    pricePerLitreCentavos = price, totalCostCentavos = (litres * price).toLong(), fullTank = input.fullTank,
                )).copy(
                    dateEpochDay = date.toEpochDay(),
                    odometerKm = odometer,
                    litres = litres,
                    pricePerLitreCentavos = price,
                    totalCostCentavos = (litres * price).toLong(),
                    fullTank = input.fullTank,
                    station = input.station.trim(),
                    notes = input.notes.trim(),
                ),
            )
            onSaved()
        }
    }

    fun delete(entry: FuelEntryEntity) = viewModelScope.launch { repository.delete(entry) }
}

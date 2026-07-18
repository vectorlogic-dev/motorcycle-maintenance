package com.motocare.app.ui.odometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.domain.model.OdometerStats
import com.motocare.app.domain.model.OdometerValidation
import com.motocare.app.domain.usecase.OdometerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class OdometerUiState(
    val motorcycle: MotorcycleEntity? = null,
    val entries: List<OdometerEntryEntity> = emptyList(),
    val stats: OdometerStats = OdometerStats(),
    val correctionPreviousKm: Long? = null,
    val error: String? = null,
)

private data class PendingReading(val km: Long, val date: LocalDate, val note: String)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OdometerViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    private val repository: OdometerRepository,
    calculator: OdometerCalculator,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id ->
        bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull()
    }
    private val entries = selected.flatMapLatest { it?.let { bike -> repository.observe(bike.id) } ?: flowOf(emptyList()) }
    private val correction = MutableStateFlow<Long?>(null)
    private val error = MutableStateFlow<String?>(null)
    private var pending: PendingReading? = null

    val uiState = combine(selected, entries, correction, error) { bike, readings, correctionKm, message ->
        OdometerUiState(
            motorcycle = bike,
            entries = readings,
            stats = calculator.stats(
                entries = readings,
                initialReadingKm = bike?.initialOdometerKm,
                initialDate = bike?.purchaseDateEpochDay?.let(LocalDate::ofEpochDay),
            ),
            correctionPreviousKm = correctionKm,
            error = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OdometerUiState())

    fun add(kmText: String, dateText: String, note: String, correctionConfirmed: Boolean = false, onSaved: () -> Unit = {}) {
        val bike = uiState.value.motorcycle ?: return
        val km = kmText.toLongOrNull()
        val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
        if (km == null || date == null) {
            error.value = "Enter a valid odometer and date."
            return
        }
        viewModelScope.launch {
            val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            when (val result = repository.addReading(bike.id, km, timestamp, note, correctionConfirmed)) {
                OdometerValidation.Valid -> {
                    correction.value = null
                    pending = null
                    error.value = null
                    onSaved()
                }
                is OdometerValidation.CorrectionRequired -> {
                    pending = PendingReading(km, date, note)
                    correction.value = result.previousKm
                }
                OdometerValidation.NegativeReading -> error.value = "Odometer cannot be negative."
            }
        }
    }

    fun confirmCorrection(onSaved: () -> Unit = {}) {
        val value = pending ?: return
        add(value.km.toString(), value.date.toString(), value.note, true, onSaved)
    }

    fun cancelCorrection() {
        pending = null
        correction.value = null
    }

    fun delete(entry: OdometerEntryEntity) = viewModelScope.launch {
        runCatching { repository.deleteReading(entry) }
            .onSuccess { error.value = null }
            .onFailure { error.value = it.message ?: "Could not delete this reading." }
    }
}

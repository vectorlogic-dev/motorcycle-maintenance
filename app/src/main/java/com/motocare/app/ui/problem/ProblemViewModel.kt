package com.motocare.app.ui.problem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.ProblemLogEntity
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.data.repository.ProblemRepository
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

data class ProblemUiState(
    val motorcycle: MotorcycleEntity? = null,
    val problems: List<ProblemLogEntity> = emptyList(),
    val isLoading: Boolean = true,
)
data class ProblemInput(val date: String, val odometerKm: String, val severity: String, val symptom: String, val description: String, val mediaUri: String?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProblemViewModel @Inject constructor(
    motorcycles: MotorcycleRepository,
    preferences: PreferencesRepository,
    private val repository: ProblemRepository,
) : ViewModel() {
    private val selected = combine(motorcycles.activeMotorcycles, preferences.selectedMotorcycleId) { bikes, id -> bikes.firstOrNull { it.id == id } ?: bikes.firstOrNull() }
    private val problems = selected.flatMapLatest { it?.let { bike -> repository.observe(bike.id) } ?: flowOf(emptyList()) }
    val uiState = combine(selected, problems) { bike, issues -> ProblemUiState(bike, issues, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProblemUiState())

    fun add(input: ProblemInput, onSaved: () -> Unit) = save(input, null, onSaved)

    fun update(existing: ProblemLogEntity, input: ProblemInput, onSaved: () -> Unit) = save(input, existing, onSaved)

    private fun save(input: ProblemInput, existing: ProblemLogEntity?, onSaved: () -> Unit) {
        val bike = uiState.value.motorcycle ?: return
        val date = runCatching { LocalDate.parse(input.date) }.getOrNull() ?: return
        viewModelScope.launch {
            repository.save(
                (existing ?: ProblemLogEntity(
                    motorcycleId = bike.id, dateEpochDay = date.toEpochDay(), odometerKm = input.odometerKm.toLongOrNull(),
                    severity = input.severity, symptom = input.symptom.trim(), description = input.description.trim(), mediaUri = input.mediaUri,
                )).copy(
                    dateEpochDay = date.toEpochDay(), odometerKm = input.odometerKm.toLongOrNull(), severity = input.severity,
                    symptom = input.symptom.trim(), description = input.description.trim(), mediaUri = input.mediaUri,
                ),
                input.mediaUri?.takeIf { it != existing?.mediaUri },
            )
            onSaved()
        }
    }

    fun resolve(problem: ProblemLogEntity, resolution: String) = viewModelScope.launch {
        repository.save(problem.copy(resolved = true, resolution = resolution.trim()), null)
    }

    fun delete(problem: ProblemLogEntity) = viewModelScope.launch { repository.delete(problem) }
}

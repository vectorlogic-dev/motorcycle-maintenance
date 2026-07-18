package com.motocare.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motocare.app.data.repository.PreferencesRepository
import com.motocare.app.data.repository.SampleDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val samples: SampleDataRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    fun startEmpty() = viewModelScope.launch { preferences.finishOnboarding() }

    fun createSample() = viewModelScope.launch {
        val id = samples.createHondaClickSample()
        preferences.selectMotorcycle(id)
        preferences.finishOnboarding()
    }
}

package com.motocare.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("motocare_preferences")

@Singleton
class PreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val selectedMotorcycleId = longPreferencesKey("selected_motorcycle_id")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val currency = stringPreferencesKey("currency")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val dateFormat = stringPreferencesKey("date_format")
        val defaultParkingCentavos = longPreferencesKey("default_parking_centavos")
        val defaultFuelPriceCentavos = longPreferencesKey("default_fuel_price_centavos")
    }

    val selectedMotorcycleId: Flow<Long?> = context.dataStore.data.map { it[Keys.selectedMotorcycleId] }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingComplete] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.notificationsEnabled] ?: true }
    val currency: Flow<String> = context.dataStore.data.map { it[Keys.currency] ?: "PHP" }
    val distanceUnit: Flow<String> = context.dataStore.data.map { it[Keys.distanceUnit] ?: "km" }
    val dateFormat: Flow<String> = context.dataStore.data.map { it[Keys.dateFormat] ?: "dd/MM/yyyy" }
    val defaultParkingCentavos: Flow<Long> = context.dataStore.data.map { it[Keys.defaultParkingCentavos] ?: 3_500L }
    val defaultFuelPriceCentavos: Flow<Long> = context.dataStore.data.map { it[Keys.defaultFuelPriceCentavos] ?: 7_000L }

    suspend fun selectMotorcycle(id: Long) {
        context.dataStore.edit { it[Keys.selectedMotorcycleId] = id }
    }

    suspend fun finishOnboarding() {
        context.dataStore.edit { it[Keys.onboardingComplete] = true }
    }

    suspend fun setDefaultParkingCentavos(amount: Long) {
        context.dataStore.edit { it[Keys.defaultParkingCentavos] = amount.coerceAtLeast(0) }
    }
}

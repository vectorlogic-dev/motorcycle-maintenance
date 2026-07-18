package com.motocare.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("motocare_preferences")

@Singleton
class PreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val selectedMotorcycleId = longPreferencesKey("selected_motorcycle_id")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val currency = stringPreferencesKey("currency")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val dateFormat = stringPreferencesKey("date_format")
        val defaultParkingCentavos = longPreferencesKey("default_parking_centavos")
        val defaultFuelPriceCentavos = longPreferencesKey("default_fuel_price_centavos")
        val staleOdometerDays = intPreferencesKey("stale_odometer_days")
        val theme = stringPreferencesKey("theme")
        val notificationDisabledMotorcycleIds = stringSetPreferencesKey("notification_disabled_motorcycle_ids")
    }

    val selectedMotorcycleId: Flow<Long?> = context.dataStore.data.map { it[Keys.selectedMotorcycleId] }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingComplete] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.notificationsEnabled] ?: true }
    val currency: Flow<String> = context.dataStore.data.map { it[Keys.currency] ?: "PHP" }
    val distanceUnit: Flow<String> = context.dataStore.data.map { it[Keys.distanceUnit] ?: "km" }
    val dateFormat: Flow<String> = context.dataStore.data.map { it[Keys.dateFormat] ?: "dd/MM/yyyy" }
    val defaultParkingCentavos: Flow<Long> = context.dataStore.data.map { it[Keys.defaultParkingCentavos] ?: 3_500L }
    val defaultFuelPriceCentavos: Flow<Long> = context.dataStore.data.map { it[Keys.defaultFuelPriceCentavos] ?: 7_000L }
    val staleOdometerDays: Flow<Int> = context.dataStore.data.map { it[Keys.staleOdometerDays] ?: 14 }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.theme] ?: "SYSTEM" }
    val notificationDisabledMotorcycleIds: Flow<Set<Long>> = context.dataStore.data.map { preferences ->
        preferences[Keys.notificationDisabledMotorcycleIds].orEmpty().mapNotNull(String::toLongOrNull).toSet()
    }

    suspend fun selectMotorcycle(id: Long) {
        context.dataStore.edit { it[Keys.selectedMotorcycleId] = id }
    }

    suspend fun finishOnboarding() {
        context.dataStore.edit { it[Keys.onboardingComplete] = true }
    }

    suspend fun setDefaultParkingCentavos(amount: Long) {
        context.dataStore.edit { it[Keys.defaultParkingCentavos] = amount.coerceAtLeast(0) }
    }

    suspend fun setDefaultFuelPriceCentavos(amount: Long) {
        context.dataStore.edit { it[Keys.defaultFuelPriceCentavos] = amount.coerceAtLeast(0) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.notificationsEnabled] = enabled }
    }

    suspend fun setStaleOdometerDays(days: Int) {
        context.dataStore.edit { it[Keys.staleOdometerDays] = days.coerceIn(1, 365) }
    }

    suspend fun setCurrency(currency: String) {
        require(currency in setOf("PHP", "USD", "EUR"))
        context.dataStore.edit { it[Keys.currency] = currency }
    }

    suspend fun setDistanceUnit(unit: String) {
        require(unit == "km")
        context.dataStore.edit { it[Keys.distanceUnit] = unit }
    }

    suspend fun setDateFormat(format: String) {
        require(format in setOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd"))
        context.dataStore.edit { it[Keys.dateFormat] = format }
    }

    suspend fun setTheme(theme: String) {
        require(theme in setOf("SYSTEM", "LIGHT", "DARK"))
        context.dataStore.edit { it[Keys.theme] = theme }
    }

    suspend fun setMotorcycleNotificationsEnabled(motorcycleId: Long, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val disabled = preferences[Keys.notificationDisabledMotorcycleIds].orEmpty().toMutableSet()
            if (enabled) disabled.remove(motorcycleId.toString()) else disabled.add(motorcycleId.toString())
            preferences[Keys.notificationDisabledMotorcycleIds] = disabled
        }
    }
}

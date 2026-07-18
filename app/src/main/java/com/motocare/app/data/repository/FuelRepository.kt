package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepository @Inject constructor(private val database: MotoCareDatabase) {
    fun observe(motorcycleId: Long): Flow<List<FuelEntryEntity>> = database.fuelDao().observeForMotorcycle(motorcycleId)

    suspend fun add(entry: FuelEntryEntity): Long = database.withTransaction {
        val id = database.fuelDao().insert(entry)
        val motorcycle = database.motorcycleDao().getById(entry.motorcycleId)
        if (motorcycle != null && entry.odometerKm > motorcycle.currentOdometerKm) {
            database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = entry.odometerKm))
            database.odometerDao().insert(
                OdometerEntryEntity(
                    motorcycleId = entry.motorcycleId,
                    readingKm = entry.odometerKm,
                    recordedAtEpochMillis = LocalDate.ofEpochDay(entry.dateEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    note = "Fuel entry",
                ),
            )
        }
        id
    }
}

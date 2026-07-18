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

    suspend fun save(entry: FuelEntryEntity): Long = database.withTransaction {
        if (entry.id != 0L) {
            database.fuelDao().getById(entry.id)?.let { removeGeneratedReading(it) }
        }
        val id = if (entry.id == 0L) database.fuelDao().insert(entry) else {
            database.fuelDao().update(entry)
            entry.id
        }
        syncCurrentOdometer(entry.motorcycleId)
        val motorcycle = database.motorcycleDao().getById(entry.motorcycleId)
        if (motorcycle != null && entry.odometerKm > motorcycle.currentOdometerKm) {
            database.odometerDao().insert(
                OdometerEntryEntity(
                    motorcycleId = entry.motorcycleId,
                    readingKm = entry.odometerKm,
                    recordedAtEpochMillis = LocalDate.ofEpochDay(entry.dateEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    note = "Fuel entry",
                ),
            )
        }
        syncCurrentOdometer(entry.motorcycleId)
        id
    }

    suspend fun add(entry: FuelEntryEntity): Long = save(entry)

    suspend fun delete(entry: FuelEntryEntity) = database.withTransaction {
        removeGeneratedReading(entry)
        database.fuelDao().delete(entry)
        syncCurrentOdometer(entry.motorcycleId)
    }

    private suspend fun removeGeneratedReading(entry: FuelEntryEntity) {
        database.odometerDao().deleteGenerated(
            motorcycleId = entry.motorcycleId,
            readingKm = entry.odometerKm,
            recordedAtEpochMillis = entry.timestamp(),
            note = "Fuel entry",
        )
    }

    private suspend fun syncCurrentOdometer(motorcycleId: Long) {
        val motorcycle = database.motorcycleDao().getById(motorcycleId) ?: return
        val latest = database.odometerDao().latest(motorcycleId)?.readingKm ?: motorcycle.initialOdometerKm
        if (latest != motorcycle.currentOdometerKm) database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = latest))
    }

    private fun FuelEntryEntity.timestamp(): Long =
        LocalDate.ofEpochDay(dateEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

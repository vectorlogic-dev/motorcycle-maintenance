package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.domain.model.OdometerValidation
import com.motocare.app.domain.usecase.OdometerCalculator
import com.motocare.app.util.asStoredDateMillis
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OdometerRepository @Inject constructor(
    private val database: MotoCareDatabase,
    private val calculator: OdometerCalculator,
) {
    fun observe(motorcycleId: Long): Flow<List<OdometerEntryEntity>> =
        database.odometerDao().observeForMotorcycle(motorcycleId)

    suspend fun addReading(
        motorcycleId: Long,
        readingKm: Long,
        recordedEpochDay: Long,
        note: String,
        correctionConfirmed: Boolean,
    ): OdometerValidation = database.withTransaction {
        val motorcycle = database.motorcycleDao().getById(motorcycleId) ?: return@withTransaction OdometerValidation.NegativeReading
        val entries = database.odometerDao().getForMotorcycle(motorcycleId)
        val unconfirmed = calculator.validateTimeline(readingKm, recordedEpochDay, entries, correctionConfirmed = false)
        val validation = calculator.validateTimeline(readingKm, recordedEpochDay, entries, correctionConfirmed)
        if (validation != OdometerValidation.Valid) return@withTransaction validation
        val date = LocalDate.ofEpochDay(recordedEpochDay)
        database.odometerDao().insert(
            OdometerEntryEntity(
                motorcycleId = motorcycleId,
                readingKm = readingKm,
                recordedAtEpochMillis = date.asStoredDateMillis(),
                recordedEpochDay = recordedEpochDay,
                note = note.trim(),
                isCorrection = unconfirmed is OdometerValidation.CorrectionRequired,
            ),
        )
        syncCurrentOdometer(motorcycleId)
        OdometerValidation.Valid
    }

    suspend fun deleteReading(entry: OdometerEntryEntity) = database.withTransaction {
        val motorcycle = database.motorcycleDao().getById(entry.motorcycleId) ?: return@withTransaction
        database.odometerDao().delete(entry)
        val latestReading = database.odometerDao().latest(entry.motorcycleId)?.readingKm ?: motorcycle.initialOdometerKm
        database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = latestReading))
    }

    private suspend fun syncCurrentOdometer(motorcycleId: Long) {
        val motorcycle = database.motorcycleDao().getById(motorcycleId) ?: return
        val latestReading = database.odometerDao().latest(motorcycleId)?.readingKm ?: motorcycle.initialOdometerKm
        if (latestReading != motorcycle.currentOdometerKm) {
            database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = latestReading))
        }
    }
}

package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.domain.model.OdometerValidation
import com.motocare.app.domain.usecase.OdometerCalculator
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
        recordedAtEpochMillis: Long,
        note: String,
        correctionConfirmed: Boolean,
    ): OdometerValidation = database.withTransaction {
        val motorcycle = database.motorcycleDao().getById(motorcycleId) ?: return@withTransaction OdometerValidation.NegativeReading
        val validation = calculator.validate(readingKm, motorcycle.currentOdometerKm, correctionConfirmed)
        if (validation != OdometerValidation.Valid) return@withTransaction validation
        database.odometerDao().insert(
            OdometerEntryEntity(
                motorcycleId = motorcycleId,
                readingKm = readingKm,
                recordedAtEpochMillis = recordedAtEpochMillis,
                note = note.trim(),
                isCorrection = readingKm < motorcycle.currentOdometerKm,
            ),
        )
        database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = readingKm))
        OdometerValidation.Valid
    }

    suspend fun deleteReading(entry: OdometerEntryEntity) = database.withTransaction {
        val motorcycle = database.motorcycleDao().getById(entry.motorcycleId) ?: return@withTransaction
        database.odometerDao().delete(entry)
        val latestReading = database.odometerDao().latest(entry.motorcycleId)?.readingKm
        database.motorcycleDao().update(
            motorcycle.copy(currentOdometerKm = latestReading ?: motorcycle.initialOdometerKm),
        )
    }
}

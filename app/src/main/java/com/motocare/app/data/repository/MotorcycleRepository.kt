package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.util.asStoredDateMillis
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotorcycleRepository @Inject constructor(private val database: MotoCareDatabase) {
    private val dao get() = database.motorcycleDao()

    val activeMotorcycles: Flow<List<MotorcycleEntity>> = dao.observeActive()
    val archivedMotorcycles: Flow<List<MotorcycleEntity>> = dao.observeArchived()
    fun observe(id: Long): Flow<MotorcycleEntity?> = dao.observeById(id)
    suspend fun get(id: Long): MotorcycleEntity? = dao.getById(id)
    suspend fun add(motorcycle: MotorcycleEntity): Long = dao.insert(motorcycle)

    suspend fun create(
        motorcycle: MotorcycleEntity,
        starterSchedules: List<MaintenanceScheduleEntity>,
    ): Long = database.withTransaction {
        val initialDate = motorcycle.initialOdometerEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
        val normalized = motorcycle.copy(initialOdometerEpochDay = initialDate.toEpochDay())
        val id = dao.insert(normalized)
        database.odometerDao().insert(
            OdometerEntryEntity(
                motorcycleId = id,
                readingKm = normalized.initialOdometerKm,
                recordedAtEpochMillis = initialDate.asStoredDateMillis(),
                recordedEpochDay = initialDate.toEpochDay(),
                note = "Starting odometer",
            ),
        )
        database.maintenanceDao().insertAll(starterSchedules.map { it.copy(motorcycleId = id) })
        id
    }

    suspend fun update(motorcycle: MotorcycleEntity) = database.withTransaction {
        motorcycle.initialOdometerEpochDay?.let { epochDay ->
            val starting = database.odometerDao().startingEntry(motorcycle.id)
            val earliestHistoryDate = database.odometerDao()
                .getForMotorcycle(motorcycle.id)
                .asSequence()
                .filter { it.id != starting?.id }
                .mapNotNull { it.recordedEpochDay }
                .minOrNull()
            require(earliestHistoryDate == null || epochDay <= earliestHistoryDate) {
                "The initial odometer date must be on or before the first history entry."
            }
        }
        dao.update(motorcycle)
        motorcycle.initialOdometerEpochDay?.let { epochDay ->
            val date = LocalDate.ofEpochDay(epochDay)
            val starting = database.odometerDao().startingEntry(motorcycle.id)
            if (starting == null) {
                database.odometerDao().insert(
                    OdometerEntryEntity(
                        motorcycleId = motorcycle.id,
                        readingKm = motorcycle.initialOdometerKm,
                        recordedAtEpochMillis = date.asStoredDateMillis(),
                        recordedEpochDay = epochDay,
                        note = "Starting odometer",
                    ),
                )
            } else {
                database.odometerDao().update(
                    starting.copy(
                        readingKm = motorcycle.initialOdometerKm,
                        recordedAtEpochMillis = date.asStoredDateMillis(),
                        recordedEpochDay = epochDay,
                    ),
                )
            }
            val current = database.odometerDao().latest(motorcycle.id)?.readingKm ?: motorcycle.initialOdometerKm
            if (current != motorcycle.currentOdometerKm) dao.update(motorcycle.copy(currentOdometerKm = current))
        }
    }

    suspend fun archive(id: Long) = dao.archive(id)
    suspend fun restore(id: Long) = dao.restore(id)
    suspend fun deletePermanently(id: Long) = dao.deleteById(id)
}

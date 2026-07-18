package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.AttachmentReferenceEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.data.local.entity.ServiceRecordItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepository @Inject constructor(private val database: MotoCareDatabase) {
    fun observe(motorcycleId: Long): Flow<List<ServiceRecordEntity>> = database.serviceDao().observeRecords(motorcycleId)

    suspend fun itemIds(serviceRecordId: Long): List<Long> = database.serviceDao().itemIds(serviceRecordId)

    suspend fun add(record: ServiceRecordEntity, scheduleIds: Set<Long>, receiptUris: List<String>): Long =
        database.withTransaction {
            val recordId = database.serviceDao().insert(record)
            if (scheduleIds.isNotEmpty()) {
                database.serviceDao().insertItems(scheduleIds.map { ServiceRecordItemEntity(recordId, it) })
            }
            scheduleIds.forEach { scheduleId ->
                val schedule = database.maintenanceDao().getById(scheduleId)
                if (schedule != null && schedule.motorcycleId == record.motorcycleId) {
                    database.maintenanceDao().update(
                        schedule.copy(
                            lastServiceEpochDay = record.serviceEpochDay,
                            lastServiceOdometerKm = record.odometerKm,
                            nextDueEpochDay = schedule.intervalDays?.let { record.serviceEpochDay + it },
                            nextDueOdometerKm = schedule.intervalKm?.let { record.odometerKm + it },
                        ),
                    )
                }
            }
            receiptUris.forEach { uri ->
                database.serviceDao().insertAttachment(
                    AttachmentReferenceEntity(ownerType = "SERVICE_RECORD", ownerId = recordId, uri = uri, mediaType = "image/*"),
                )
            }
            val motorcycle = database.motorcycleDao().getById(record.motorcycleId)
            if (motorcycle != null && record.odometerKm > motorcycle.currentOdometerKm) {
                database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = record.odometerKm))
                database.odometerDao().insert(
                    OdometerEntryEntity(
                        motorcycleId = record.motorcycleId,
                        readingKm = record.odometerKm,
                        recordedAtEpochMillis = java.time.LocalDate.ofEpochDay(record.serviceEpochDay)
                            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        note = "Service record",
                    ),
                )
            }
            recordId
        }

    suspend fun update(record: ServiceRecordEntity, scheduleIds: Set<Long>, receiptUris: List<String>) =
        database.withTransaction {
            val old = database.serviceDao().getById(record.id) ?: return@withTransaction
            val affectedSchedules = database.serviceDao().itemIds(record.id).toSet() + scheduleIds
            removeGeneratedReading(old)
            database.serviceDao().update(record)
            database.serviceDao().deleteItems(record.id)
            if (scheduleIds.isNotEmpty()) {
                database.serviceDao().insertItems(scheduleIds.map { ServiceRecordItemEntity(record.id, it) })
            }
            receiptUris.forEach { uri ->
                database.serviceDao().insertAttachment(
                    AttachmentReferenceEntity(ownerType = "SERVICE_RECORD", ownerId = record.id, uri = uri, mediaType = "image/*"),
                )
            }
            affectedSchedules.forEach { syncSchedule(it) }
            syncCurrentOdometer(record.motorcycleId)
            addGeneratedReadingIfCurrent(record)
            syncCurrentOdometer(record.motorcycleId)
        }

    suspend fun delete(record: ServiceRecordEntity) = database.withTransaction {
        val scheduleIds = database.serviceDao().itemIds(record.id)
        removeGeneratedReading(record)
        database.phaseThreeDao().deleteAttachments("SERVICE_RECORD", record.id)
        database.serviceDao().delete(record)
        scheduleIds.forEach { syncSchedule(it) }
        syncCurrentOdometer(record.motorcycleId)
    }

    private suspend fun syncSchedule(scheduleId: Long) {
        val schedule = database.maintenanceDao().getById(scheduleId) ?: return
        val latest = database.serviceDao().latestForSchedule(scheduleId)
        database.maintenanceDao().update(
            schedule.copy(
                lastServiceEpochDay = latest?.serviceEpochDay,
                lastServiceOdometerKm = latest?.odometerKm,
                nextDueEpochDay = latest?.let { record -> schedule.intervalDays?.let { record.serviceEpochDay + it } },
                nextDueOdometerKm = latest?.let { record -> schedule.intervalKm?.let { record.odometerKm + it } },
            ),
        )
    }

    private suspend fun removeGeneratedReading(record: ServiceRecordEntity) {
        database.odometerDao().deleteGenerated(
            motorcycleId = record.motorcycleId,
            readingKm = record.odometerKm,
            recordedAtEpochMillis = record.timestamp(),
            note = "Service record",
        )
    }

    private suspend fun addGeneratedReadingIfCurrent(record: ServiceRecordEntity) {
        val motorcycle = database.motorcycleDao().getById(record.motorcycleId) ?: return
        if (record.odometerKm > motorcycle.currentOdometerKm) {
            database.odometerDao().insert(
                OdometerEntryEntity(
                    motorcycleId = record.motorcycleId,
                    readingKm = record.odometerKm,
                    recordedAtEpochMillis = record.timestamp(),
                    note = "Service record",
                ),
            )
        }
    }

    private suspend fun syncCurrentOdometer(motorcycleId: Long) {
        val motorcycle = database.motorcycleDao().getById(motorcycleId) ?: return
        val latest = database.odometerDao().latest(motorcycleId)?.readingKm ?: motorcycle.initialOdometerKm
        if (latest != motorcycle.currentOdometerKm) database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = latest))
    }

    private fun ServiceRecordEntity.timestamp(): Long = java.time.LocalDate.ofEpochDay(serviceEpochDay)
        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}

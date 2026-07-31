package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.AttachmentReferenceEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.data.local.entity.ServiceRecordItemEntity
import com.motocare.app.util.asStoredDateMillis
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepository @Inject constructor(private val database: MotoCareDatabase) {
    fun observe(motorcycleId: Long): Flow<List<ServiceRecordEntity>> = database.serviceDao().observeRecords(motorcycleId)

    suspend fun itemIds(serviceRecordId: Long): List<Long> = database.serviceDao().itemIds(serviceRecordId)
    suspend fun attachments(serviceRecordId: Long): List<AttachmentReferenceEntity> =
        database.attachmentDao().getForOwner("SERVICE_RECORD", serviceRecordId)

    suspend fun add(record: ServiceRecordEntity, scheduleIds: Set<Long>, receiptUris: List<String>): Long =
        database.withTransaction {
            val validScheduleIds = requireSchedulesForMotorcycle(scheduleIds, record.motorcycleId)
            val recordId = database.serviceDao().insert(record)
            if (validScheduleIds.isNotEmpty()) {
                database.serviceDao().insertItems(validScheduleIds.map { ServiceRecordItemEntity(recordId, it) })
            }
            validScheduleIds.forEach { scheduleId ->
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
            receiptUris.distinct().forEach { uri ->
                database.attachmentDao().insert(
                    AttachmentReferenceEntity(ownerType = "SERVICE_RECORD", ownerId = recordId, uri = uri, mediaType = "image/*"),
                )
            }
            addGeneratedReading(record)
            syncCurrentOdometer(record.motorcycleId)
            recordId
        }

    suspend fun update(record: ServiceRecordEntity, scheduleIds: Set<Long>, receiptUris: List<String>) =
        database.withTransaction {
            val old = database.serviceDao().getById(record.id) ?: return@withTransaction
            require(old.motorcycleId == record.motorcycleId) { "A service record cannot be moved to another motorcycle." }
            val validScheduleIds = requireSchedulesForMotorcycle(scheduleIds, record.motorcycleId)
            val affectedSchedules = database.serviceDao().itemIds(record.id).toSet() + validScheduleIds
            removeGeneratedReading(old)
            database.serviceDao().update(record)
            database.serviceDao().deleteItems(record.id)
            if (validScheduleIds.isNotEmpty()) {
                database.serviceDao().insertItems(validScheduleIds.map { ServiceRecordItemEntity(record.id, it) })
            }
            database.attachmentDao().deleteForOwner("SERVICE_RECORD", record.id)
            receiptUris.distinct().forEach { uri ->
                database.attachmentDao().insert(
                    AttachmentReferenceEntity(ownerType = "SERVICE_RECORD", ownerId = record.id, uri = uri, mediaType = "image/*"),
                )
            }
            affectedSchedules.forEach { syncSchedule(it) }
            syncCurrentOdometer(record.motorcycleId)
            addGeneratedReading(record)
            syncCurrentOdometer(record.motorcycleId)
        }

    suspend fun delete(record: ServiceRecordEntity) = database.withTransaction {
        val scheduleIds = database.serviceDao().itemIds(record.id)
        removeGeneratedReading(record)
        database.attachmentDao().deleteForOwner("SERVICE_RECORD", record.id)
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

    private suspend fun requireSchedulesForMotorcycle(scheduleIds: Set<Long>, motorcycleId: Long): Set<Long> {
        val valid = scheduleIds.filterTo(mutableSetOf()) { id ->
            database.maintenanceDao().getById(id)?.motorcycleId == motorcycleId
        }
        require(valid.size == scheduleIds.size) { "A selected maintenance item belongs to another motorcycle." }
        return valid
    }

    private suspend fun removeGeneratedReading(record: ServiceRecordEntity) {
        database.odometerDao().deleteGenerated(
            motorcycleId = record.motorcycleId,
            readingKm = record.odometerKm,
            recordedEpochDay = record.serviceEpochDay,
            note = "Service record",
        )
    }

    private suspend fun addGeneratedReading(record: ServiceRecordEntity) {
        database.odometerDao().insert(
            OdometerEntryEntity(
                motorcycleId = record.motorcycleId,
                readingKm = record.odometerKm,
                recordedAtEpochMillis = java.time.LocalDate.ofEpochDay(record.serviceEpochDay).asStoredDateMillis(),
                recordedEpochDay = record.serviceEpochDay,
                note = "Service record",
            ),
        )
    }

    private suspend fun syncCurrentOdometer(motorcycleId: Long) {
        val motorcycle = database.motorcycleDao().getById(motorcycleId) ?: return
        val latest = database.odometerDao().latest(motorcycleId)?.readingKm ?: motorcycle.initialOdometerKm
        if (latest != motorcycle.currentOdometerKm) database.motorcycleDao().update(motorcycle.copy(currentOdometerKm = latest))
    }
}

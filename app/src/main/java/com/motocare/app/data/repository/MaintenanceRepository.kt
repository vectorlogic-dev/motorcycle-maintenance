package com.motocare.app.data.repository

import com.motocare.app.data.local.dao.MaintenanceDao
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepository @Inject constructor(private val dao: MaintenanceDao) {
    fun observeActive(motorcycleId: Long): Flow<List<MaintenanceScheduleEntity>> = dao.observeActive(motorcycleId)
    suspend fun getAllActive(): List<MaintenanceScheduleEntity> = dao.getAllActive()
    suspend fun add(schedule: MaintenanceScheduleEntity): Long = dao.insert(schedule)
    suspend fun update(schedule: MaintenanceScheduleEntity) = dao.update(schedule)
    suspend fun deactivate(id: Long) = dao.deactivate(id)
}

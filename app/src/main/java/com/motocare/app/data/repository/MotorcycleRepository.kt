package com.motocare.app.data.repository

import com.motocare.app.data.local.dao.MotorcycleDao
import com.motocare.app.data.local.entity.MotorcycleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotorcycleRepository @Inject constructor(private val dao: MotorcycleDao) {
    val activeMotorcycles: Flow<List<MotorcycleEntity>> = dao.observeActive()
    fun observe(id: Long): Flow<MotorcycleEntity?> = dao.observeById(id)
    suspend fun get(id: Long): MotorcycleEntity? = dao.getById(id)
    suspend fun add(motorcycle: MotorcycleEntity): Long = dao.insert(motorcycle)
    suspend fun update(motorcycle: MotorcycleEntity) = dao.update(motorcycle)
    suspend fun archive(id: Long) = dao.archive(id)
}

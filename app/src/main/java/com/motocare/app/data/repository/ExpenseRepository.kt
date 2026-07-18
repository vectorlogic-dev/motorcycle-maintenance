package com.motocare.app.data.repository

import com.motocare.app.data.local.dao.ExpenseDao
import com.motocare.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(private val dao: ExpenseDao) {
    fun observe(motorcycleId: Long): Flow<List<ExpenseEntity>> = dao.observeForMotorcycle(motorcycleId)
    suspend fun add(expense: ExpenseEntity): Long = dao.insert(expense)
    suspend fun addParking(motorcycleId: Long, amountCentavos: Long, date: LocalDate = LocalDate.now()): Long = dao.insert(
        ExpenseEntity(
            motorcycleId = motorcycleId,
            dateEpochDay = date.toEpochDay(),
            category = "PARKING",
            amountCentavos = amountCentavos,
            description = "Office parking",
        ),
    )
}

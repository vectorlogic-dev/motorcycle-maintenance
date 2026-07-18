package com.motocare.app.data.repository

import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.data.local.entity.InsuranceRecordEntity
import com.motocare.app.data.local.entity.RegistrationRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComplianceRepository @Inject constructor(private val database: MotoCareDatabase) {
    private val dao get() = database.phaseThreeDao()
    fun observeCoverage(motorcycleId: Long): Flow<CoveragePlanEntity?> = dao.observeCoverage(motorcycleId)
    fun observeRegistration(motorcycleId: Long): Flow<RegistrationRecordEntity?> = dao.observeRegistration(motorcycleId)
    fun observeInsurance(motorcycleId: Long): Flow<InsuranceRecordEntity?> = dao.observeInsurance(motorcycleId)

    suspend fun saveCoverage(plan: CoveragePlanEntity): Long = if (plan.id == 0L) dao.insertCoverage(plan) else {
        dao.updateCoverage(plan); plan.id
    }

    suspend fun saveRegistration(record: RegistrationRecordEntity): Long = database.withTransaction {
        val id = if (record.id == 0L) dao.insertRegistration(record) else { dao.updateRegistration(record); record.id }
        database.motorcycleDao().getById(record.motorcycleId)?.let { bike ->
            database.motorcycleDao().update(bike.copy(registrationExpiryEpochDay = record.expiryEpochDay, plateNumber = record.plateNumber))
        }
        id
    }

    suspend fun saveInsurance(record: InsuranceRecordEntity): Long = database.withTransaction {
        val id = if (record.id == 0L) dao.insertInsurance(record) else { dao.updateInsurance(record); record.id }
        database.motorcycleDao().getById(record.motorcycleId)?.let { bike ->
            database.motorcycleDao().update(bike.copy(insuranceExpiryEpochDay = record.expiryEpochDay))
        }
        id
    }

    suspend fun allCoverage(): List<CoveragePlanEntity> = dao.getAllCoverage()
    suspend fun allRegistrations(): List<RegistrationRecordEntity> = dao.getAllRegistrations()
    suspend fun allInsurance(): List<InsuranceRecordEntity> = dao.getAllInsurance()
}

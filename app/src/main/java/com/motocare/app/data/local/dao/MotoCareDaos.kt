package com.motocare.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.data.local.entity.AttachmentReferenceEntity
import com.motocare.app.data.local.entity.ExpenseEntity
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.data.local.entity.ServiceRecordItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorcycleDao {
    @Query("SELECT * FROM motorcycles WHERE archived = 0 ORDER BY createdAtEpochMillis, id")
    fun observeActive(): Flow<List<MotorcycleEntity>>

    @Query("SELECT * FROM motorcycles WHERE id = :id")
    fun observeById(id: Long): Flow<MotorcycleEntity?>

    @Query("SELECT * FROM motorcycles WHERE id = :id")
    suspend fun getById(id: Long): MotorcycleEntity?

    @Insert
    suspend fun insert(motorcycle: MotorcycleEntity): Long

    @Update
    suspend fun update(motorcycle: MotorcycleEntity)

    @Query("UPDATE motorcycles SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_entries WHERE motorcycleId = :motorcycleId ORDER BY recordedAtEpochMillis DESC, id DESC")
    fun observeForMotorcycle(motorcycleId: Long): Flow<List<OdometerEntryEntity>>

    @Query("SELECT * FROM odometer_entries WHERE motorcycleId = :motorcycleId ORDER BY recordedAtEpochMillis DESC, id DESC LIMIT 1")
    suspend fun latest(motorcycleId: Long): OdometerEntryEntity?

    @Insert
    suspend fun insert(entry: OdometerEntryEntity): Long

    @Delete
    suspend fun delete(entry: OdometerEntryEntity)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_schedules WHERE motorcycleId = :motorcycleId AND active = 1 ORDER BY name")
    fun observeActive(motorcycleId: Long): Flow<List<MaintenanceScheduleEntity>>

    @Query("SELECT * FROM maintenance_schedules WHERE active = 1")
    suspend fun getAllActive(): List<MaintenanceScheduleEntity>

    @Insert
    suspend fun insert(schedule: MaintenanceScheduleEntity): Long

    @Insert
    suspend fun insertAll(schedules: List<MaintenanceScheduleEntity>)

    @Query("SELECT * FROM maintenance_schedules WHERE id = :id")
    suspend fun getById(id: Long): MaintenanceScheduleEntity?

    @Update
    suspend fun update(schedule: MaintenanceScheduleEntity)

    @Query("UPDATE maintenance_schedules SET active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}

@Dao
interface PhaseTwoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert
    suspend fun insertCoverage(plan: CoveragePlanEntity): Long

    @Query("SELECT * FROM coverage_plans WHERE motorcycleId = :motorcycleId ORDER BY id DESC LIMIT 1")
    fun observeCoverage(motorcycleId: Long): Flow<CoveragePlanEntity?>

    @Query("SELECT * FROM loans WHERE motorcycleId = :motorcycleId LIMIT 1")
    fun observeLoan(motorcycleId: Long): Flow<LoanEntity?>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service_records WHERE motorcycleId = :motorcycleId ORDER BY serviceEpochDay DESC, id DESC")
    fun observeRecords(motorcycleId: Long): Flow<List<ServiceRecordEntity>>

    @Query("SELECT maintenanceScheduleId FROM service_record_items WHERE serviceRecordId = :serviceRecordId")
    suspend fun itemIds(serviceRecordId: Long): List<Long>

    @Insert
    suspend fun insert(record: ServiceRecordEntity): Long

    @Insert
    suspend fun insertItems(items: List<ServiceRecordItemEntity>)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentReferenceEntity): Long
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE motorcycleId = :motorcycleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeForMotorcycle(motorcycleId: Long): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE motorcycleId = :motorcycleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeForMotorcycle(motorcycleId: Long): Flow<List<FuelEntryEntity>>

    @Insert
    suspend fun insert(entry: FuelEntryEntity): Long
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE motorcycleId = :motorcycleId LIMIT 1")
    fun observeForMotorcycle(motorcycleId: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE motorcycleId = :motorcycleId LIMIT 1")
    suspend fun getForMotorcycle(motorcycleId: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY installmentNumber")
    fun observePayments(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY installmentNumber")
    suspend fun getPayments(loanId: Long): List<LoanPaymentEntity>

    @Insert
    suspend fun insertPayments(payments: List<LoanPaymentEntity>)

    @Update
    suspend fun updatePayment(payment: LoanPaymentEntity)
}

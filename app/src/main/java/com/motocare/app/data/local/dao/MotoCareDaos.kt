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
import com.motocare.app.data.local.entity.InsuranceRecordEntity
import com.motocare.app.data.local.entity.ProblemLogEntity
import com.motocare.app.data.local.entity.RegistrationRecordEntity
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

    @Query("DELETE FROM odometer_entries WHERE motorcycleId = :motorcycleId AND readingKm = :readingKm AND recordedAtEpochMillis = :recordedAtEpochMillis AND note = :note")
    suspend fun deleteGenerated(motorcycleId: Long, readingKm: Long, recordedAtEpochMillis: Long, note: String)
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

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun getById(id: Long): ServiceRecordEntity?

    @Query("SELECT sr.* FROM service_records sr INNER JOIN service_record_items sri ON sri.serviceRecordId = sr.id WHERE sri.maintenanceScheduleId = :scheduleId ORDER BY sr.serviceEpochDay DESC, sr.id DESC LIMIT 1")
    suspend fun latestForSchedule(scheduleId: Long): ServiceRecordEntity?

    @Insert
    suspend fun insert(record: ServiceRecordEntity): Long

    @Update
    suspend fun update(record: ServiceRecordEntity)

    @Delete
    suspend fun delete(record: ServiceRecordEntity)

    @Insert
    suspend fun insertItems(items: List<ServiceRecordItemEntity>)

    @Query("DELETE FROM service_record_items WHERE serviceRecordId = :serviceRecordId")
    suspend fun deleteItems(serviceRecordId: Long)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentReferenceEntity): Long
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE motorcycleId = :motorcycleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeForMotorcycle(motorcycleId: Long): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE motorcycleId = :motorcycleId ORDER BY dateEpochDay DESC, id DESC")
    fun observeForMotorcycle(motorcycleId: Long): Flow<List<FuelEntryEntity>>

    @Insert
    suspend fun insert(entry: FuelEntryEntity): Long

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun getById(id: Long): FuelEntryEntity?

    @Update
    suspend fun update(entry: FuelEntryEntity)

    @Delete
    suspend fun delete(entry: FuelEntryEntity)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans")
    suspend fun getAllLoans(): List<LoanEntity>

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

@Dao
interface PhaseThreeDao {
    @Query("SELECT * FROM coverage_plans WHERE motorcycleId = :motorcycleId ORDER BY id DESC LIMIT 1")
    fun observeCoverage(motorcycleId: Long): Flow<CoveragePlanEntity?>

    @Query("SELECT * FROM coverage_plans")
    suspend fun getAllCoverage(): List<CoveragePlanEntity>

    @Insert
    suspend fun insertCoverage(plan: CoveragePlanEntity): Long

    @Update
    suspend fun updateCoverage(plan: CoveragePlanEntity)

    @Query("SELECT * FROM registration_records WHERE motorcycleId = :motorcycleId ORDER BY id DESC LIMIT 1")
    fun observeRegistration(motorcycleId: Long): Flow<RegistrationRecordEntity?>

    @Query("SELECT * FROM registration_records")
    suspend fun getAllRegistrations(): List<RegistrationRecordEntity>

    @Insert
    suspend fun insertRegistration(record: RegistrationRecordEntity): Long

    @Update
    suspend fun updateRegistration(record: RegistrationRecordEntity)

    @Query("SELECT * FROM insurance_records WHERE motorcycleId = :motorcycleId ORDER BY id DESC LIMIT 1")
    fun observeInsurance(motorcycleId: Long): Flow<InsuranceRecordEntity?>

    @Query("SELECT * FROM insurance_records")
    suspend fun getAllInsurance(): List<InsuranceRecordEntity>

    @Insert
    suspend fun insertInsurance(record: InsuranceRecordEntity): Long

    @Update
    suspend fun updateInsurance(record: InsuranceRecordEntity)

    @Query("SELECT * FROM problem_logs WHERE motorcycleId = :motorcycleId ORDER BY resolved, dateEpochDay DESC, id DESC")
    fun observeProblems(motorcycleId: Long): Flow<List<ProblemLogEntity>>

    @Query("SELECT * FROM problem_logs WHERE resolved = 0")
    suspend fun getAllUnresolvedProblems(): List<ProblemLogEntity>

    @Insert
    suspend fun insertProblem(problem: ProblemLogEntity): Long

    @Update
    suspend fun updateProblem(problem: ProblemLogEntity)

    @Delete
    suspend fun deleteProblem(problem: ProblemLogEntity)

    @Query("DELETE FROM attachment_references WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteAttachments(ownerType: String, ownerId: Long)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentReferenceEntity): Long
}

package com.motocare.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
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

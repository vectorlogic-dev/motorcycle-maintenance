package com.motocare.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.motocare.app.data.local.dao.MaintenanceDao
import com.motocare.app.data.local.dao.MotorcycleDao
import com.motocare.app.data.local.dao.OdometerDao
import com.motocare.app.data.local.dao.PhaseTwoDao
import com.motocare.app.data.local.entity.AttachmentReferenceEntity
import com.motocare.app.data.local.entity.CoveragePlanEntity
import com.motocare.app.data.local.entity.ExpenseEntity
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.InsuranceRecordEntity
import com.motocare.app.data.local.entity.LoanEntity
import com.motocare.app.data.local.entity.LoanPaymentEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.data.local.entity.ProblemLogEntity
import com.motocare.app.data.local.entity.RegistrationRecordEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.data.local.entity.ServiceRecordItemEntity

@Database(
    entities = [
        MotorcycleEntity::class,
        OdometerEntryEntity::class,
        MaintenanceScheduleEntity::class,
        ServiceRecordEntity::class,
        ServiceRecordItemEntity::class,
        ExpenseEntity::class,
        FuelEntryEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        RegistrationRecordEntity::class,
        InsuranceRecordEntity::class,
        ProblemLogEntity::class,
        CoveragePlanEntity::class,
        AttachmentReferenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MotoCareDatabase : RoomDatabase() {
    abstract fun motorcycleDao(): MotorcycleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun phaseTwoDao(): PhaseTwoDao
}

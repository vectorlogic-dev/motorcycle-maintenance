package com.motocare.app.di

import android.content.Context
import androidx.room.Room
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.Migrations
import com.motocare.app.data.local.dao.MaintenanceDao
import com.motocare.app.data.local.dao.ExpenseDao
import com.motocare.app.data.local.dao.FuelDao
import com.motocare.app.data.local.dao.LoanDao
import com.motocare.app.data.local.dao.MotorcycleDao
import com.motocare.app.data.local.dao.OdometerDao
import com.motocare.app.data.local.dao.PhaseTwoDao
import com.motocare.app.data.local.dao.ServiceDao
import com.motocare.app.data.local.dao.PhaseThreeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MotoCareDatabase =
        Room.databaseBuilder(context, MotoCareDatabase::class.java, "motocare.db")
            .addMigrations(Migrations.MIGRATION_1_2)
            .build()

    @Provides fun motorcycleDao(db: MotoCareDatabase): MotorcycleDao = db.motorcycleDao()
    @Provides fun odometerDao(db: MotoCareDatabase): OdometerDao = db.odometerDao()
    @Provides fun maintenanceDao(db: MotoCareDatabase): MaintenanceDao = db.maintenanceDao()
    @Provides fun phaseTwoDao(db: MotoCareDatabase): PhaseTwoDao = db.phaseTwoDao()
    @Provides fun serviceDao(db: MotoCareDatabase): ServiceDao = db.serviceDao()
    @Provides fun expenseDao(db: MotoCareDatabase): ExpenseDao = db.expenseDao()
    @Provides fun fuelDao(db: MotoCareDatabase): FuelDao = db.fuelDao()
    @Provides fun loanDao(db: MotoCareDatabase): LoanDao = db.loanDao()
    @Provides fun phaseThreeDao(db: MotoCareDatabase): PhaseThreeDao = db.phaseThreeDao()
}

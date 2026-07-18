package com.motocare.app.di

import android.content.Context
import androidx.room.Room
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.dao.MaintenanceDao
import com.motocare.app.data.local.dao.MotorcycleDao
import com.motocare.app.data.local.dao.OdometerDao
import com.motocare.app.data.local.dao.PhaseTwoDao
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
        Room.databaseBuilder(context, MotoCareDatabase::class.java, "motocare.db").build()

    @Provides fun motorcycleDao(db: MotoCareDatabase): MotorcycleDao = db.motorcycleDao()
    @Provides fun odometerDao(db: MotoCareDatabase): OdometerDao = db.odometerDao()
    @Provides fun maintenanceDao(db: MotoCareDatabase): MaintenanceDao = db.maintenanceDao()
    @Provides fun phaseTwoDao(db: MotoCareDatabase): PhaseTwoDao = db.phaseTwoDao()
}

package com.motocare.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.data.repository.SampleDataRepository
import com.motocare.app.domain.model.OdometerValidation
import com.motocare.app.domain.usecase.OdometerCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RepositoryOperationsTest {
    private lateinit var database: MotoCareDatabase
    private lateinit var repository: MotorcycleRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MotoCareDatabase::class.java).build()
        repository = MotorcycleRepository(database.motorcycleDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun motorcycleRepository_addUpdateAndArchive() = runTest {
        val id = repository.add(
            MotorcycleEntity(
                name = "Daily bike",
                manufacturer = "Honda",
                model = "BeAT",
                initialOdometerKm = 10,
                currentOdometerKm = 10,
            ),
        )

        val added = repository.get(id)
        assertNotNull(added)
        repository.update(added!!.copy(currentOdometerKm = 125))
        assertEquals(125L, repository.get(id)?.currentOdometerKm)

        repository.archive(id)
        assertEquals(emptyList<MotorcycleEntity>(), repository.activeMotorcycles.first())
        assertEquals(true, repository.get(id)?.archived)
    }

    @Test
    fun odometerRepository_deleteRecalculatesCurrentReading() = runTest {
        val id = repository.add(
            MotorcycleEntity(
                name = "Daily bike",
                manufacturer = "Honda",
                model = "BeAT",
                initialOdometerKm = 1,
                currentOdometerKm = 1,
            ),
        )
        val odometers = OdometerRepository(database, OdometerCalculator())
        assertEquals(OdometerValidation.Valid, odometers.addReading(id, 25, 1_000, "", false))
        assertEquals(OdometerValidation.Valid, odometers.addReading(id, 40, 2_000, "", false))

        odometers.deleteReading(requireNotNull(database.odometerDao().latest(id)))
        assertEquals(25L, repository.get(id)?.currentOdometerKm)
        odometers.deleteReading(requireNotNull(database.odometerDao().latest(id)))
        assertEquals(1L, repository.get(id)?.currentOdometerKm)
    }

    @Test
    fun sampleProfile_startsOdometerOnJuly16() = runTest {
        val id = SampleDataRepository(database).createHondaClickSample()
        val bike = requireNotNull(repository.get(id))
        val initialReading = requireNotNull(database.odometerDao().latest(id))
        val readingDate = Instant.ofEpochMilli(initialReading.recordedAtEpochMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()

        assertEquals(LocalDate.of(2026, 7, 16).toEpochDay(), bike.purchaseDateEpochDay)
        assertEquals(LocalDate.of(2026, 7, 16), readingDate)
        assertEquals(1L, initialReading.readingKm)
    }
}

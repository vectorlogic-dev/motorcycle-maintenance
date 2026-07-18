package com.motocare.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motocare.app.data.local.MotoCareDatabase
import com.motocare.app.backup.BackupRepository
import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.ExpenseEntity
import com.motocare.app.data.local.entity.FuelEntryEntity
import com.motocare.app.data.local.entity.MaintenanceScheduleEntity
import com.motocare.app.data.local.entity.ProblemLogEntity
import com.motocare.app.data.local.entity.ServiceRecordEntity
import com.motocare.app.data.repository.ExpenseRepository
import com.motocare.app.data.repository.FuelRepository
import com.motocare.app.data.repository.ProblemRepository
import com.motocare.app.data.repository.ServiceRepository
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

    @Test
    fun historyRepositories_updateDeleteAndRecalculateDependencies() = runTest {
        val motorcycleId = repository.add(
            MotorcycleEntity(
                name = "Daily bike", manufacturer = "Honda", model = "BeAT",
                initialOdometerKm = 1, currentOdometerKm = 1,
            ),
        )
        val expenses = ExpenseRepository(database.expenseDao())
        val expenseId = expenses.add(ExpenseEntity(motorcycleId = motorcycleId, dateEpochDay = 1, category = "OTHER", amountCentavos = 100))
        val expense = expenses.observe(motorcycleId).first().single().copy(id = expenseId, amountCentavos = 250)
        expenses.update(expense)
        assertEquals(250L, expenses.observe(motorcycleId).first().single().amountCentavos)
        expenses.delete(expense)
        assertEquals(emptyList<ExpenseEntity>(), expenses.observe(motorcycleId).first())

        val fuel = FuelRepository(database)
        val fuelId = fuel.save(
            FuelEntryEntity(
                motorcycleId = motorcycleId, dateEpochDay = 2, odometerKm = 10, litres = 2.0,
                pricePerLitreCentavos = 7_000, totalCostCentavos = 14_000, fullTank = true,
            ),
        )
        val fill = fuel.observe(motorcycleId).first().single().copy(id = fuelId, dateEpochDay = 3, odometerKm = 20)
        fuel.save(fill)
        assertEquals(20L, repository.get(motorcycleId)?.currentOdometerKm)
        fuel.delete(fill)
        assertEquals(1L, repository.get(motorcycleId)?.currentOdometerKm)

        val problems = ProblemRepository(database)
        val problemId = problems.save(
            ProblemLogEntity(motorcycleId = motorcycleId, dateEpochDay = 2, severity = "LOW", symptom = "Noise"),
            null,
        )
        val problem = problems.observe(motorcycleId).first().single().copy(id = problemId, symptom = "CVT noise")
        problems.save(problem, null)
        assertEquals("CVT noise", problems.observe(motorcycleId).first().single().symptom)
        problems.delete(problem)
        assertEquals(emptyList<ProblemLogEntity>(), problems.observe(motorcycleId).first())

        val scheduleId = database.maintenanceDao().insert(
            MaintenanceScheduleEntity(motorcycleId = motorcycleId, name = "Oil", intervalKm = 1_000),
        )
        val services = ServiceRepository(database)
        val serviceId = services.add(
            ServiceRecordEntity(motorcycleId = motorcycleId, serviceEpochDay = 4, odometerKm = 30),
            setOf(scheduleId),
            emptyList(),
        )
        val service = services.observe(motorcycleId).first().single().copy(id = serviceId, serviceEpochDay = 5, odometerKm = 40)
        services.update(service, setOf(scheduleId), emptyList())
        assertEquals(40L, repository.get(motorcycleId)?.currentOdometerKm)
        assertEquals(1_040L, database.maintenanceDao().getById(scheduleId)?.nextDueOdometerKm)
        services.delete(service)
        assertEquals(1L, repository.get(motorcycleId)?.currentOdometerKm)
        assertEquals(null, database.maintenanceDao().getById(scheduleId)?.lastServiceOdometerKm)
    }

    @Test
    fun backupRestore_upgradesVersionOneMotorcycles() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BackupRepository(context, database).restoreJsonText(
            """
            {
              "format":"MotoCare backup",
              "schemaVersion":1,
              "tables":{"motorcycles":[{
                "id":7,"name":"Legacy bike","manufacturer":"Honda","model":"Click","variant":"",
                "year":null,"purchaseDateEpochDay":null,"initialOdometerKm":12,"currentOdometerKm":20,
                "plateNumber":"","engineNumber":"","chassisNumber":"","registrationExpiryEpochDay":null,
                "insuranceExpiryEpochDay":null,"isFinanced":1,"notes":"","photoUri":null,"archived":0,
                "createdAtEpochMillis":0
              }]}
            }
            """.trimIndent(),
        )

        val restored = requireNotNull(repository.get(7))
        assertEquals("FINANCED", restored.purchaseType)
        assertEquals(null, restored.purchasePriceCentavos)
        assertEquals("", restored.seller)
        assertEquals(false, restored.secondHand)
    }

    @Test
    fun backupRestore_rollsBackWhenAnyRowIsInvalid() = runTest {
        val existingId = repository.add(
            MotorcycleEntity(
                name = "Keep me", manufacturer = "Honda", model = "BeAT",
                initialOdometerKm = 1, currentOdometerKm = 1,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val failure = runCatching {
            BackupRepository(context, database).restoreJsonText(
                """
                {
                  "format":"MotoCare backup",
                  "schemaVersion":2,
                  "tables":{"motorcycles":[{"id":9,"name":null}]}
                }
                """.trimIndent(),
            )
        }

        assertEquals(true, failure.isFailure)
        assertEquals("Keep me", repository.get(existingId)?.name)
    }
}

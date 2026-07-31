package com.motocare.app.data

import android.content.Context
import android.net.Uri
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
import com.motocare.app.data.repository.MaintenanceRepository
import com.motocare.app.data.repository.ProblemRepository
import com.motocare.app.data.repository.ServiceRepository
import com.motocare.app.data.repository.MotorcycleRepository
import com.motocare.app.data.repository.OdometerRepository
import com.motocare.app.domain.model.OdometerValidation
import com.motocare.app.domain.usecase.OdometerCalculator
import com.motocare.app.domain.usecase.StarterMaintenanceScheduleFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RepositoryOperationsTest {
    private lateinit var database: MotoCareDatabase
    private lateinit var repository: MotorcycleRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MotoCareDatabase::class.java).build()
        repository = MotorcycleRepository(database)
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
    fun starterMaintenanceSchedules_arePersistedAsEditableTemplates() = runTest {
        val motorcycleId = repository.add(
            MotorcycleEntity(
                name = "Daily bike",
                manufacturer = "Honda",
                model = "BeAT",
                initialOdometerKm = 8_500,
                currentOdometerKm = 8_500,
            ),
        )
        val maintenance = MaintenanceRepository(database.maintenanceDao())
        maintenance.addAll(
            StarterMaintenanceScheduleFactory().create(
                motorcycleId = motorcycleId,
                currentOdometerKm = 8_500,
                driveType = "CHAIN",
                coolingType = "AIR",
                startDate = java.time.LocalDate.of(2026, 7, 31),
            ),
        )

        val schedules = maintenance.observeActive(motorcycleId).first()
        assertEquals(6, schedules.size)
        assertEquals(6, schedules.count { it.isEditableTemplate && it.source == "RESEARCH_STARTER_V1" })
        assertEquals(11_500L, schedules.single { it.name == "Engine oil change" }.nextDueOdometerKm)
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
    fun odometerRepository_backdatedReadingDoesNotReplaceChronologicalCurrentReading() = runTest {
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
        assertEquals(OdometerValidation.Valid, odometers.addReading(id, 100, 20_000, "", false))
        assertEquals(OdometerValidation.Valid, odometers.addReading(id, 50, 19_990, "", false))
        assertEquals(100L, repository.get(id)?.currentOdometerKm)

        val conflict = odometers.addReading(id, 150, 19_995, "", false)
        assertTrue(conflict is OdometerValidation.CorrectionRequired)
        assertEquals(100L, repository.get(id)?.currentOdometerKm)
    }

    @Test
    fun motorcycleRepository_createPersistsStartingReadingAndSchedulesAtomically() = runTest {
        val initialDate = java.time.LocalDate.of(2026, 7, 16)
        val id = repository.create(
            MotorcycleEntity(
                name = "Daily bike",
                manufacturer = "Honda",
                model = "Click",
                initialOdometerKm = 12,
                initialOdometerEpochDay = initialDate.toEpochDay(),
                currentOdometerKm = 12,
            ),
            listOf(MaintenanceScheduleEntity(motorcycleId = 0, name = "Engine oil")),
        )

        assertEquals(initialDate.toEpochDay(), repository.get(id)?.initialOdometerEpochDay)
        assertEquals(initialDate.toEpochDay(), database.odometerDao().latest(id)?.recordedEpochDay)
        assertEquals("Engine oil", database.maintenanceDao().getAllForMotorcycle(id).single().name)
    }

    @Test
    fun motorcycleRepository_rejectsInitialDateAfterExistingHistory() = runTest {
        val id = repository.create(
            MotorcycleEntity(
                name = "Daily bike",
                manufacturer = "Honda",
                model = "Click",
                initialOdometerKm = 10,
                initialOdometerEpochDay = 19_990,
                currentOdometerKm = 10,
            ),
            emptyList(),
        )
        assertEquals(
            OdometerValidation.Valid,
            OdometerRepository(database, OdometerCalculator()).addReading(id, 20, 20_000, "", false),
        )

        val failure = runCatching {
            repository.update(requireNotNull(repository.get(id)).copy(initialOdometerEpochDay = 20_001))
        }

        assertTrue(failure.isFailure)
        assertEquals(19_990L, repository.get(id)?.initialOdometerEpochDay)
        assertEquals(20L, repository.get(id)?.currentOdometerKm)
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
            MaintenanceScheduleEntity(motorcycleId = motorcycleId, name = "Oil", intervalKm = 1_000, intervalDays = 30),
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
        assertEquals(35L, database.maintenanceDao().getById(scheduleId)?.nextDueEpochDay)
        services.delete(service)
        assertEquals(1L, repository.get(motorcycleId)?.currentOdometerKm)
        assertEquals(null, database.maintenanceDao().getById(scheduleId)?.lastServiceOdometerKm)
    }

    @Test
    fun serviceRepository_rejectsScheduleFromAnotherMotorcycle() = runTest {
        val first = repository.add(
            MotorcycleEntity(
                name = "First", manufacturer = "Honda", model = "Click",
                initialOdometerKm = 1, currentOdometerKm = 1,
            ),
        )
        val second = repository.add(
            MotorcycleEntity(
                name = "Second", manufacturer = "Yamaha", model = "NMAX",
                initialOdometerKm = 1, currentOdometerKm = 1,
            ),
        )
        val secondSchedule = database.maintenanceDao().insert(
            MaintenanceScheduleEntity(motorcycleId = second, name = "Belt"),
        )

        val failure = runCatching {
            ServiceRepository(database).add(
                ServiceRecordEntity(motorcycleId = first, serviceEpochDay = 20_000, odometerKm = 10),
                setOf(secondSchedule),
                emptyList(),
            )
        }

        assertTrue(failure.isFailure)
        assertEquals(emptyList<ServiceRecordEntity>(), ServiceRepository(database).observe(first).first())
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
        assertEquals("UNKNOWN", restored.driveType)
        assertEquals("UNKNOWN", restored.coolingType)
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

    @Test
    fun backupRestore_upgradesVersionThreeOdometerDates() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BackupRepository(context, database).restoreJsonText(
            """
            {
              "format":"MotoCare backup",
              "schemaVersion":3,
              "tables":{
                "motorcycles":[{
                  "id":3,"name":"Legacy","manufacturer":"Honda","model":"Click","variant":"","year":null,
                  "purchaseDateEpochDay":20650,"purchaseType":"CASH","purchasePriceCentavos":null,"seller":"",
                  "secondHand":0,"driveType":"BELT","coolingType":"LIQUID","initialOdometerKm":10,
                  "currentOdometerKm":40,"plateNumber":"","engineNumber":"","chassisNumber":"",
                  "registrationExpiryEpochDay":null,"insuranceExpiryEpochDay":null,"isFinanced":0,
                  "notes":"","photoUri":null,"archived":0,"createdAtEpochMillis":0
                }],
                "odometer_entries":[{
                  "id":4,"motorcycleId":3,"readingKm":40,"recordedAtEpochMillis":1784131200000,
                  "note":"Reading","isCorrection":0
                }]
              }
            }
            """.trimIndent(),
        )

        assertEquals(20650L, repository.get(3)?.initialOdometerEpochDay)
        assertNotNull(database.odometerDao().latest(3)?.recordedEpochDay)
    }

    @Test
    fun backupVersionFour_roundTripsRecordsAndAttachmentReferences() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val motorcycleId = repository.create(
            MotorcycleEntity(
                name = "Backup bike",
                manufacturer = "Honda",
                model = "Click",
                initialOdometerKm = 10,
                initialOdometerEpochDay = 20_000,
                currentOdometerKm = 10,
            ),
            emptyList(),
        )
        val serviceId = ServiceRepository(database).add(
            ServiceRecordEntity(motorcycleId = motorcycleId, serviceEpochDay = 20_001, odometerKm = 20),
            emptySet(),
            listOf("content://example/receipt", "content://example/receipt"),
        )
        assertEquals(1, database.attachmentDao().getForOwner("SERVICE_RECORD", serviceId).size)
        val file = File.createTempFile("motocare-backup-", ".json", context.cacheDir)
        val uri = Uri.fromFile(file)
        val backups = BackupRepository(context, database)

        try {
            backups.writeJson(uri)
            assertTrue(file.readText().contains("\"schemaVersion\": 4"))
            backups.restoreJson(uri)

            assertEquals("Backup bike", repository.get(motorcycleId)?.name)
            assertEquals(20_000L, repository.get(motorcycleId)?.initialOdometerEpochDay)
            assertEquals(
                "content://example/receipt",
                database.attachmentDao().getForOwner("SERVICE_RECORD", serviceId).single().uri,
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun backupRestore_rejectsAttachmentsWithoutMatchingOwners() = runTest {
        val existingId = repository.add(
            MotorcycleEntity(
                name = "Keep me",
                manufacturer = "Honda",
                model = "BeAT",
                initialOdometerKm = 1,
                currentOdometerKm = 1,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<Context>()

        val failure = runCatching {
            BackupRepository(context, database).restoreJsonText(
                """
                {
                  "format":"MotoCare backup",
                  "schemaVersion":4,
                  "tables":{
                    "attachment_references":[{
                      "id":1,"ownerType":"SERVICE_RECORD","ownerId":99,
                      "uri":"content://example/orphan","mediaType":"image/*"
                    }]
                  }
                }
                """.trimIndent(),
            )
        }

        assertTrue(failure.isFailure)
        assertEquals("Keep me", repository.get(existingId)?.name)
    }
}

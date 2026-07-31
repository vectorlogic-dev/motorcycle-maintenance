package com.motocare.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.motocare.app.data.local.Migrations
import com.motocare.app.data.local.MotoCareDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MotoCareDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_preservesMotorcycleAndAddsOwnershipFields() {
        val databaseName = "migration-test-1-2"
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                """INSERT INTO motorcycles
                    (id,name,manufacturer,model,variant,year,purchaseDateEpochDay,initialOdometerKm,currentOdometerKm,
                    plateNumber,engineNumber,chassisNumber,registrationExpiryEpochDay,insuranceExpiryEpochDay,isFinanced,
                    notes,photoUri,archived,createdAtEpochMillis)
                    VALUES (1,'Click','Honda','Click125','',NULL,NULL,1,1,'','','',NULL,NULL,1,'',NULL,0,0)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 2, true, Migrations.MIGRATION_1_2).use { db ->
            db.query("SELECT purchaseType,purchasePriceCentavos,seller,secondHand FROM motorcycles WHERE id=1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("FINANCED", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals("", cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
            }
        }
    }

    @Test
    fun migrate2To3_preservesMotorcycleAndAddsEquipmentProfile() {
        val databaseName = "migration-test-2-3"
        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                """INSERT INTO motorcycles
                    (id,name,manufacturer,model,variant,year,purchaseDateEpochDay,purchaseType,purchasePriceCentavos,
                    seller,secondHand,initialOdometerKm,currentOdometerKm,plateNumber,engineNumber,chassisNumber,
                    registrationExpiryEpochDay,insuranceExpiryEpochDay,isFinanced,notes,photoUri,archived,createdAtEpochMillis)
                    VALUES (1,'NMAX','Yamaha','NMAX','',NULL,NULL,'CASH',NULL,'',0,1,1,'','','',NULL,NULL,0,'',NULL,0,0)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 3, true, Migrations.MIGRATION_2_3).use { db ->
            db.query("SELECT name,driveType,coolingType FROM motorcycles WHERE id=1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("NMAX", cursor.getString(0))
                assertEquals("UNKNOWN", cursor.getString(1))
                assertEquals("UNKNOWN", cursor.getString(2))
            }
        }
    }
}

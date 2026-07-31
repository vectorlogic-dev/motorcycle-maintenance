package com.motocare.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN purchaseType TEXT NOT NULL DEFAULT 'UNKNOWN'")
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN purchasePriceCentavos INTEGER")
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN seller TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN secondHand INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE motorcycles SET purchaseType = CASE WHEN isFinanced = 1 THEN 'FINANCED' ELSE 'UNKNOWN' END")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN driveType TEXT NOT NULL DEFAULT 'UNKNOWN'")
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN coolingType TEXT NOT NULL DEFAULT 'UNKNOWN'")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE motorcycles ADD COLUMN initialOdometerEpochDay INTEGER")
            db.execSQL("UPDATE motorcycles SET initialOdometerEpochDay = purchaseDateEpochDay WHERE purchaseDateEpochDay IS NOT NULL")
            db.execSQL("ALTER TABLE odometer_entries ADD COLUMN recordedEpochDay INTEGER")
            db.execSQL(
                """
                UPDATE odometer_entries
                SET recordedEpochDay = CAST(
                    julianday(date(recordedAtEpochMillis / 1000, 'unixepoch', 'localtime')) -
                    julianday('1970-01-01') AS INTEGER
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_odometer_entries_motorcycleId_recordedEpochDay " +
                    "ON odometer_entries (motorcycleId, recordedEpochDay)",
            )
        }
    }
}

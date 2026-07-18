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
}

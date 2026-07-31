package com.motocare.app.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.motocare.app.data.local.MotoCareDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: MotoCareDatabase,
) {
    private val tables = listOf(
        "motorcycles",
        "odometer_entries",
        "maintenance_schedules",
        "service_records",
        "service_record_items",
        "expenses",
        "fuel_entries",
        "loans",
        "loan_payments",
        "registration_records",
        "insurance_records",
        "problem_logs",
        "coverage_plans",
        "attachment_references",
    )

    suspend fun writeJson(uri: Uri) = withContext(Dispatchers.IO) {
        val tableData = database.withTransaction {
            val db = database.openHelper.readableDatabase
            JSONObject().also { snapshot ->
                tables.forEach { table ->
                    db.query("SELECT * FROM $table").use { cursor -> snapshot.put(table, cursor.toJson()) }
                }
            }
        }
        val root = JSONObject()
            .put("format", "MotoCare backup")
            .put("schemaVersion", 4)
            .put("exportedAt", Instant.now().toString())
            .put("tables", tableData)
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(root.toString(2)) }
            ?: error("Unable to open backup destination")
    }

    suspend fun restoreJson(uri: Uri) = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to open backup")
        restoreJsonText(text)
    }

    internal suspend fun restoreJsonText(text: String) {
        val root = JSONObject(text)
        require(root.optString("format") == "MotoCare backup") { "Not a MotoCare backup" }
        val schemaVersion = root.optInt("schemaVersion")
        require(schemaVersion in 1..4) { "Unsupported backup version" }
        val data = root.getJSONObject("tables")
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            tables.asReversed().forEach { db.delete(it, null, null) }
            tables.forEach { table ->
                val rows = data.optJSONArray(table) ?: JSONArray()
                repeat(rows.length()) { index ->
                    val row = rows.getJSONObject(index)
                    if (table == "motorcycles") {
                        if (schemaVersion == 1) row.upgradeMotorcycleFromV1()
                        if (schemaVersion <= 2) row.upgradeMotorcycleFromV2()
                        if (schemaVersion <= 3) row.upgradeMotorcycleFromV3()
                    }
                    if (table == "odometer_entries" && schemaVersion <= 3) row.upgradeOdometerFromV3()
                    val values = ContentValues()
                    row.keys().forEach { key -> values.putJson(key, row.get(key)) }
                    check(db.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values) != -1L) {
                        "Could not restore $table row ${index + 1}"
                    }
                }
            }
            validateAttachmentOwners(db)
            db.query("PRAGMA foreign_key_check").use { cursor ->
                check(!cursor.moveToFirst()) { "Backup contains broken record relationships" }
            }
        }
        database.invalidationTracker.refreshAsync()
    }

    private fun validateAttachmentOwners(db: SupportSQLiteDatabase) {
        val orphanCount = db.query(
            """
            SELECT COUNT(*)
            FROM attachment_references AS attachment
            WHERE
                (attachment.ownerType = 'SERVICE_RECORD' AND NOT EXISTS (
                    SELECT 1 FROM service_records WHERE id = attachment.ownerId
                ))
                OR (attachment.ownerType = 'PROBLEM' AND NOT EXISTS (
                    SELECT 1 FROM problem_logs WHERE id = attachment.ownerId
                ))
                OR attachment.ownerType NOT IN ('SERVICE_RECORD', 'PROBLEM')
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }
        check(orphanCount == 0L) { "Backup contains attachments without matching records" }
    }

    private fun JSONObject.upgradeMotorcycleFromV1() {
        if (!has("purchaseType")) put("purchaseType", if (optInt("isFinanced") != 0) "FINANCED" else "UNKNOWN")
        if (!has("purchasePriceCentavos")) put("purchasePriceCentavos", JSONObject.NULL)
        if (!has("seller")) put("seller", "")
        if (!has("secondHand")) put("secondHand", false)
    }

    private fun JSONObject.upgradeMotorcycleFromV2() {
        if (!has("driveType")) put("driveType", "UNKNOWN")
        if (!has("coolingType")) put("coolingType", "UNKNOWN")
    }

    private fun JSONObject.upgradeMotorcycleFromV3() {
        if (!has("initialOdometerEpochDay")) {
            put(
                "initialOdometerEpochDay",
                if (has("purchaseDateEpochDay") && !isNull("purchaseDateEpochDay")) getLong("purchaseDateEpochDay") else JSONObject.NULL,
            )
        }
    }

    private fun JSONObject.upgradeOdometerFromV3() {
        if (!has("recordedEpochDay")) {
            val millis = optLong("recordedAtEpochMillis")
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            put("recordedEpochDay", date.toEpochDay())
        }
    }

    suspend fun writeCsv(uri: Uri, table: String) = withContext(Dispatchers.IO) {
        require(table in setOf("expenses", "fuel_entries", "service_records", "odometer_entries"))
        val db = database.openHelper.readableDatabase
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            db.query("SELECT * FROM $table ORDER BY id").use { cursor ->
                writer.appendLine(cursor.columnNames.joinToString(",") { csvCell(it) })
                while (cursor.moveToNext()) {
                    writer.appendLine(cursor.columnNames.indices.joinToString(",") { index ->
                        csvCell(if (cursor.isNull(index)) "" else cursor.getString(index))
                    })
                }
            }
        } ?: error("Unable to open export destination")
    }

    private fun Cursor.toJson(): JSONArray = JSONArray().also { rows ->
        while (moveToNext()) {
            val row = JSONObject()
            columnNames.forEachIndexed { index, name ->
                val value: Any = when (getType(index)) {
                    Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                    Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                    Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
                    Cursor.FIELD_TYPE_STRING -> getString(index)
                    else -> error("Unsupported value in $name")
                }
                row.put(name, value)
            }
            rows.put(row)
        }
    }

    private fun ContentValues.putJson(key: String, value: Any?) {
        when (value) {
            null, JSONObject.NULL -> putNull(key)
            is Int -> put(key, value)
            is Long -> put(key, value)
            is Double -> put(key, value)
            is Boolean -> put(key, if (value) 1 else 0)
            else -> put(key, value.toString())
        }
    }

    private fun csvCell(raw: String): String {
        val protected = if (raw.firstOrNull() in listOf('=', '+', '-', '@')) "'$raw" else raw
        return "\"${protected.replace("\"", "\"\"")}\""
    }
}

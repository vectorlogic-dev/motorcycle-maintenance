package com.motocare.app.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.room.withTransaction
import com.motocare.app.data.local.MotoCareDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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
        val db = database.openHelper.readableDatabase
        val tableData = JSONObject()
        tables.forEach { table ->
            db.query("SELECT * FROM $table").use { cursor -> tableData.put(table, cursor.toJson()) }
        }
        val root = JSONObject()
            .put("format", "MotoCare backup")
            .put("schemaVersion", 2)
            .put("exportedAt", Instant.now().toString())
            .put("tables", tableData)
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(root.toString(2)) }
            ?: error("Unable to open backup destination")
    }

    suspend fun restoreJson(uri: Uri) = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to open backup")
        val root = JSONObject(text)
        require(root.optString("format") == "MotoCare backup") { "Not a MotoCare backup" }
        require(root.optInt("schemaVersion") in 1..2) { "Unsupported backup version" }
        val data = root.getJSONObject("tables")
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            tables.asReversed().forEach { db.delete(it, null, null) }
            tables.forEach { table ->
                val rows = data.optJSONArray(table) ?: JSONArray()
                repeat(rows.length()) { index ->
                    val row = rows.getJSONObject(index)
                    val values = ContentValues()
                    row.keys().forEach { key -> values.putJson(key, row.get(key)) }
                    check(db.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values) != -1L) {
                        "Could not restore $table row ${index + 1}"
                    }
                }
            }
        }
        database.invalidationTracker.refreshAsync()
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

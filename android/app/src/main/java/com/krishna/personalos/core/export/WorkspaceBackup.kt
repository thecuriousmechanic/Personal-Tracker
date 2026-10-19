package com.krishna.personalos.core.export

import android.database.Cursor
import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.FieldKind
import com.krishna.personalos.core.model.TemplateDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.time.Instant

/** Portable, versioned logical backup. SQL identifiers always come from our schema, never the file. */
class WorkspaceBackup(private val database: AppDatabase, private val context: Context? = null) {
    data class Preview(val json: String, val records: Int, val exportedAt: String)

    suspend fun write(output: OutputStream) = withContext(Dispatchers.IO) {
        val tables = database.withTransaction { snapshot() }
        if (context != null) {
            val preferences = tables.getJSONArray("workspace_preferences")
            val savedKeys = (0 until preferences.length()).map { preferences.getJSONObject(it).getString("key") }.toSet()
            val pages = tables.getJSONArray("template_pages")
            val covers = (0 until pages.length()).map { pages.getJSONObject(it).getString("cover") }.filter(String::isNotBlank).distinct()
            covers.filter { "cover:$it" !in savedKeys }.forEach { cover ->
                val bytes = runCatching { context.assets.open("private-notion/$cover").use { readLimited(it) } }.getOrNull()
                if (bytes != null) preferences.put(JSONObject().put("key", "cover:$cover").put("value", Base64.encodeToString(bytes, Base64.NO_WRAP)))
            }
        }
        val document =
            JSONObject().put("format", FORMAT).put("version", 1).put("databaseVersion", 5)
                .put("exportedAt", Instant.now().toString()).put("tables", tables)
        val encoded = document.toString().toByteArray(Charsets.UTF_8)
        require(encoded.size <= MAX_BYTES) { "Workspace exceeds the 32 MB backup limit" }
        output.use { it.write(encoded) }
    }

    suspend fun preview(input: InputStream): Preview = withContext(Dispatchers.IO) {
        val bytes = readLimited(input)
        val json = bytes.toString(Charsets.UTF_8)
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT && root.optInt("version") == 1 && root.optInt("databaseVersion") == 5) { "Choose a supported Personal-Tracker backup" }
        val tables = root.getJSONObject("tables")
        Instant.parse(root.getString("exportedAt"))
        database.withTransaction { validate(tables, complete = true) }
        Preview(json, TABLES.sumOf { tables.getJSONArray(it).length() }, root.getString("exportedAt"))
    }

    suspend fun restore(preview: Preview) = withContext(Dispatchers.IO) {
        val root = JSONObject(preview.json)
        require(root.getString("format") == FORMAT && root.getInt("version") == 1 && root.getInt("databaseVersion") == 5)
        val tables = root.getJSONObject("tables")
        database.withTransaction {
            validate(tables, complete = true)
            // An import either commits every table or leaves every existing record intact.
            TABLES.reversed().forEach { database.openHelper.writableDatabase.execSQL("DELETE FROM `$it`") }
            insertRows(tables)
            database.recoveryDao().savePreference(WorkspacePreferenceEntity("bundled_import_disabled", "true"))
        }
        database.invalidationTracker.refreshAsync()
    }

    internal fun snapshot(queries: Map<String, Pair<String, Array<out Any?>>>? = null): JSONObject {
        val result = JSONObject()
        (queries?.keys ?: TABLES).forEach { table ->
            require(table in TABLES)
            val selection = queries?.get(table)
            val rows = JSONArray()
            database.openHelper.writableDatabase.query("SELECT * FROM `$table`" + (selection?.let { " WHERE ${it.first}" } ?: ""), selection?.second ?: emptyArray()).use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    cursor.columnNames.forEachIndexed { index, column ->
                        row.put(column, when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                            else -> error("Unsupported backup column")
                        })
                    }
                    rows.put(row)
                }
            }
            result.put(table, rows)
        }
        return result
    }

    internal fun insertRows(tables: JSONObject, replace: Boolean = false) {
        tables.keys().forEach { table ->
            require(table in TABLES)
            val columns = columns(table).keys.toList()
            val placeholders = columns.joinToString(",") { "?" }
            val sql = "INSERT ${if (replace) "OR REPLACE " else ""}INTO `$table` (${columns.joinToString(",") { "`$it`" }}) VALUES ($placeholders)"
            val rows = tables.getJSONArray(table)
            repeat(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                val values = columns.map { column -> row.get(column).takeUnless { it == JSONObject.NULL } }.toTypedArray()
                database.openHelper.writableDatabase.execSQL(sql, values)
            }
        }
    }

    private data class Column(val type: String, val required: Boolean, val primary: Boolean)
    private fun readLimited(input: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (buffer.size() <= MAX_BYTES) {
            val count = input.read(chunk, 0, minOf(chunk.size, MAX_BYTES + 1 - buffer.size()))
            if (count < 0) break
            buffer.write(chunk, 0, count)
        }
        require(buffer.size() <= MAX_BYTES) { "Backup is larger than 32 MB" }
        return buffer.toByteArray()
    }
    private fun columns(table: String): Map<String, Column> {
        require(table in TABLES)
        return buildMap {
            database.openHelper.writableDatabase.query("PRAGMA table_info(`$table`)").use { cursor ->
                while (cursor.moveToNext()) put(cursor.getString(1), Column(cursor.getString(2), cursor.getInt(3) != 0, cursor.getInt(5) != 0))
            }
        }
    }

    internal fun validate(tables: JSONObject, complete: Boolean) {
        val names = tables.keys().asSequence().toSet()
        require(names.all { it in TABLES } && (!complete || names == TABLES.toSet())) { "Backup tables do not match this app version" }
        var total = 0
        names.forEach { table ->
            val schema = columns(table)
            val primaryKeys = mutableSetOf<List<Any>>()
            val rows = tables.getJSONArray(table)
            total += rows.length()
            require(total <= 100_000) { "Backup contains too many records" }
            repeat(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                require(row.keys().asSequence().toSet() == schema.keys) { "Invalid fields in $table" }
                schema.forEach { (column, rule) ->
                    val value = row.get(column)
                    require(value != JSONObject.NULL || !rule.required) { "Missing $table.$column" }
                    if (value != JSONObject.NULL) when (rule.type) {
                        "TEXT" -> require(value is String) { "Invalid text in $table.$column" }
                        "INTEGER" -> require(value is Int || value is Long) { "Invalid number in $table.$column" }
                        else -> error("Unsupported schema type")
                    }
                }
                require(primaryKeys.add(schema.filterValues { it.primary }.keys.map { row.get(it) })) { "Duplicate records in $table" }
                when (table) {
                    "tasks" -> {
                        TaskStatus.valueOf(row.getString("status")); Priority.valueOf(row.getString("priority"))
                        require(row.getInt("repeatDays") in listOf(0, 1, 7)) { "Invalid repeat interval" }
                        require(row.isNull("reminderMinutes") || row.getInt("reminderMinutes") in 0..1439) { "Invalid reminder time" }
                        require(row.isNull("reminderMinutes") || !row.isNull("dueEpochDay")) { "A reminder needs a date" }
                    }
                    "projects" -> ProjectStatus.valueOf(row.getString("status"))
                    "goals" -> require(row.getInt("progress") in 0..100)
                    "entity_relations" -> RelationType.valueOf(row.getString("type"))
                    "activity_events" -> ActivityType.valueOf(row.getString("type"))
                    "tracker_fields" -> FieldKind.valueOf(row.getString("kind"))
                    "template_pages" -> TemplateDocument.parse(row.getString("data"))
                    "workspace_preferences" -> {
                        if (row.getString("key").startsWith("cover:")) Base64.decode(row.getString("value"), Base64.NO_WRAP)
                        if (row.getString("key") == "currency_symbol") require(row.getString("value").let { it.isNotBlank() && it.length <= 4 && it.none(Char::isISOControl) })
                        if (row.getString("key") == "dark_mode") require(row.getString("value") in listOf("true", "false"))
                    }
                }
            }
        }
        if (complete) {
            fun rows(table: String) = tables.getJSONArray(table).let { array -> (0 until array.length()).map { array.getJSONObject(it) } }
            fun ids(table: String) = rows(table).map { it.getString("id") }.toSet()
            val trackers = ids("trackers"); val projects = ids("projects"); val habits = ids("habits")
            val fields = rows("tracker_fields").associate { it.getString("id") to it.getString("trackerId") }
            val entries = rows("tracker_entries").associate { it.getString("id") to it.getString("trackerId") }
            require(fields.values.all { it in trackers } && entries.values.all { it in trackers }) { "Backup has missing tracker pages" }
            require(rows("tracker_cells").all { fields[it.getString("fieldId")] != null && fields[it.getString("fieldId")] == entries[it.getString("entryId")] }) { "Backup has invalid tracker links" }
            require(rows("tasks").all { it.isNull("projectId") || it.getString("projectId") in projects }) { "Backup has missing projects" }
            require(rows("habit_logs").all { it.getString("habitId") in habits }) { "Backup has missing habits" }
        }
    }

    companion object {
        private const val FORMAT = "personal-tracker-workspace"
        private const val MAX_BYTES = 32 * 1024 * 1024
        internal val TABLES = listOf("tasks", "habits", "habit_logs", "projects", "goals", "notes", "entity_relations", "activity_events", "trackers", "tracker_fields", "tracker_entries", "tracker_cells", "template_pages", "workspace_preferences", "workspace_trash")
    }
}

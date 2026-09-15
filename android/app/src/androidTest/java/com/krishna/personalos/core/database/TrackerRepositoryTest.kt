package com.krishna.personalos.core.database

import android.content.Context
import androidx.room.Room
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.krishna.personalos.core.repository.TrackerRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class TrackerRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var repository: TrackerRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TrackerRepository(database)
    }
    @After fun tearDown() { database.close() }

    @Test fun templateCopyOwnsItsEntriesAndFields() = runBlocking {
        val source = repository.create("weekly", "Routine")
        val field = database.trackerDao().fields(source).first { it.kind == "CHECKBOX" }
        val entry = TrackerEntryEntity("routine", source, "Read", createdAt = 1)
        repository.saveEntry(entry, mapOf(field.id to "true"))
        val template = repository.duplicate(source, "My template", true, true)
        val copy = repository.duplicate(template, "Next week", false, true)
        val copiedEntry = database.trackerDao().entries(copy).single()
        val copiedField = database.trackerDao().fields(copy).first { it.name == field.name }
        assertNotEquals(entry.id, copiedEntry.id)
        assertNotEquals(field.id, copiedField.id)
        repository.toggleCheckbox(copiedEntry.id, copiedField.id)
        assertEquals("true", database.trackerDao().cells(entry.id).first { it.fieldId == field.id }.value)
        assertEquals("false", database.trackerDao().cells(copiedEntry.id).first { it.fieldId == copiedField.id }.value)
        repository.archiveEntry(copiedEntry.id, true)
        repository.archiveEntry(copiedEntry.id, false)
        assertFalse(database.trackerDao().entry(copiedEntry.id)!!.archived)
        repository.saveField(copiedField.copy(archived = true))
        repository.saveField(copiedField.copy(archived = false, name = "Renamed"))
        assertEquals("false", database.trackerDao().cells(copiedEntry.id).first { it.fieldId == copiedField.id }.value)
    }

    @Test fun invalidEditRollsBackWithoutChangingSavedEntry() = runBlocking {
        val tracker = repository.create("expenses", "Spending")
        val amount = database.trackerDao().fields(tracker).first { it.name == "Amount" }
        val entry = TrackerEntryEntity("purchase", tracker, "Book", createdAt = 1)
        repository.saveEntry(entry, mapOf(amount.id to "12.50"))
        assertTrue(runCatching { repository.saveEntry(entry.copy(title = "Bad change"), mapOf(amount.id to "invalid")) }.isFailure)
        assertEquals("Book", database.trackerDao().entry(entry.id)!!.title)
        assertEquals("12.50", database.trackerDao().cells(entry.id).first { it.fieldId == amount.id }.value)
    }

    @Test fun deletingEntryRemovesItAndItsCells() = runBlocking {
        val tracker = repository.create("expenses", "Spending")
        val amount = database.trackerDao().fields(tracker).first { it.name == "Amount" }
        repository.saveEntry(TrackerEntryEntity("purchase", tracker, "Book", createdAt = 1), mapOf(amount.id to "12.50"))

        repository.deleteEntry("purchase")

        assertNull(database.trackerDao().entry("purchase"))
        assertTrue(database.trackerDao().cells("purchase").isEmpty())
    }

    @Test fun deletingMultipleEntriesRemovesOnlyTheSelectionAndItsCells() = runBlocking {
        val tracker = repository.create("expenses", "Spending")
        val amount = database.trackerDao().fields(tracker).first { it.name == "Amount" }
        listOf("one", "two", "keep").forEachIndexed { index, id ->
            repository.saveEntry(TrackerEntryEntity(id, tracker, id, createdAt = index.toLong()), mapOf(amount.id to (index + 1).toString()))
        }

        repository.deleteEntries(setOf("one", "two"))

        assertEquals(listOf("keep"), database.trackerDao().entries(tracker).map { it.id })
        assertTrue(database.trackerDao().cells("one").isEmpty())
        assertTrue(database.trackerDao().cells("two").isEmpty())
        assertFalse(database.trackerDao().cells("keep").isEmpty())
    }

    @Test fun diskReopenRetainsCustomizedFieldsAndValues() = runBlocking {
        val name = "tracker-reopen-test.db"
        context.deleteDatabase(name)
        val disk = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val repo = TrackerRepository(disk)
            val tracker = repo.create("blank", "My collection")
            repo.saveField(TrackerFieldEntity("custom", tracker, "Source", "TEXT", position = 0))
            repo.saveEntry(TrackerEntryEntity("saved", tracker, "An idea", "Reflection", 1), mapOf("custom" to "A book"))
        } finally { disk.close() }
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals("Reflection", reopened.trackerDao().entry("saved")!!.body)
            assertEquals("A book", reopened.trackerDao().cells("saved").single().value)
        } finally { reopened.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationPreservesVersionOneTasksAndNotes() = runBlocking {
        val name = "tracker-migration-test"
        context.deleteDatabase(name)
        val schema = InstrumentationRegistry.getInstrumentation().context.assets.open("com.krishna.personalos.core.database.AppDatabase/1.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        // Open the actual exported v1 schema, then let Room run and validate the production migration.
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                fun sql(value: String) = value.replace("\${TABLE_NAME}", entity.getString("tableName"))
                execSQL(sql(entity.getString("createSql")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) execSQL(sql(indices.getJSONObject(j).getString("createSql")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) execSQL(setup.getString(i))
            execSQL("INSERT INTO tasks VALUES ('old-task','Existing task','INBOX','HIGH',NULL,NULL,1,NULL,NULL)")
            execSQL("INSERT INTO notes VALUES ('old-note','Existing note','Original body',1,1)")
            version = 1
            close()
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals("Existing task", migrated.appDao().task("old-task")!!.title)
            assertEquals("Original body", migrated.appDao().observeNotes().first().single().body)
            val newTracker = TrackerRepository(migrated).create("blank", "Works after migration")
            assertNotNull(migrated.trackerDao().tracker(newTracker))
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}

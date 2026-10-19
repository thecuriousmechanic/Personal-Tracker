package com.krishna.personalos.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.krishna.personalos.core.export.WorkspaceBackup
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkspaceSafetyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun close() { db.close(); TemplateDocumentCache.clear() }

    @Test fun fullBackupRoundTripsPagesRecordsPreferencesTombstonesAndTrash() = runBlocking {
        val repo = PersonalOSRepository(db)
        repo.saveProject(ProjectEntity("project", "Study", "Personal", ProjectStatus.ACTIVE, null, 1))
        repo.saveTask(TaskEntity("task", "Read", TaskStatus.UPCOMING, Priority.HIGH, LocalDate.now().plusDays(1).toEpochDay(), "project", 1, repeatDays = 7, reminderMinutes = 540))
        repo.saveNote(NoteEntity("note", "Private note", "- [x] Saved", 1, 1))
        repo.saveGoal(GoalEntity("goal", "Finish", "Quarter", 30, 1))
        db.appDao().insertHabit(HabitEntity("habit", "Walk", 5, 1, archived = true))
        db.appDao().insertHabitLog(HabitLogEntity("habit", 100, 1))
        repeat(25) { db.appDao().insertActivity(ActivityEventEntity("event-$it", ActivityType.CREATED, "task", "Read", it.toLong())) }
        val templates = TemplateRepository(db)
        val page = templates.create("Budget", "", "finance")
        templates.saveRow(page, "expenses", PageRow("expense", "Books", mapOf("Date" to "2026-10-12", "Amount" to "50"), archived = true))
        val deletedPage = templates.create("Recover me", "", "routine")
        templates.delete(deletedPage)
        val trackers = TrackerRepository(db)
        val tracker = trackers.create("habits", "Habits")
        val field = db.trackerDao().fields(tracker).first { it.kind == "CHECKBOX" }
        trackers.saveEntry(TrackerEntryEntity("entry", tracker, "Today", createdAt = 1), mapOf(field.id to "true"))
        db.recoveryDao().savePreference(WorkspacePreferenceEntity("currency_symbol", "₹"))
        db.recoveryDao().savePreference(WorkspacePreferenceEntity("dark_mode", "true"))
        RecoveryRepository(db).pin(page, true)

        val output = ByteArrayOutputStream()
        WorkspaceBackup(db).write(output)
        val other = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            PersonalOSRepository(other).saveNote(NoteEntity("extra", "Replace me", "", 1, 1))
            val transfer = WorkspaceBackup(other)
            val preview = transfer.preview(ByteArrayInputStream(output.toByteArray()))
            assertTrue(preview.records > 25)
            transfer.restore(preview)
            assertNull(other.appDao().note("extra"))
            assertEquals(7, other.appDao().task("task")!!.repeatDays)
            assertEquals(540, other.appDao().task("task")!!.reminderMinutes)
            assertEquals("Private note", other.appDao().note("note")!!.title)
            assertEquals("₹", other.recoveryDao().preference("currency_symbol")!!.value)
            assertEquals("true", other.recoveryDao().preference("pin:$page")!!.value)
            assertEquals("true", other.recoveryDao().preference("bundled_import_disabled")!!.value)
            assertEquals("true", other.trackerDao().cells("entry").first { it.fieldId == field.id }.value)
            assertTrue(other.templatePageDao().page(deletedPage)!!.deleted)
            assertTrue(TemplateDocument.parse(other.templatePageDao().page(page)!!.data).table("expenses").rows.single().archived)
            val recovery = RecoveryRepository(other)
            recovery.restore(recovery.trash.first().single().id)
            assertFalse(other.templatePageDao().page(deletedPage)!!.deleted)
            other.withTransaction { other.openHelper.writableDatabase.query("SELECT COUNT(*) FROM activity_events").use { assertTrue(it.moveToFirst()); assertEquals(25, it.getInt(0)) } }
        } finally { other.close() }
    }

    @Test fun malformedBackupCannotReplaceCurrentWorkspace() = runBlocking {
        val repo = PersonalOSRepository(db)
        repo.saveTask(TaskEntity("task", "Keep me", TaskStatus.TODAY, Priority.HIGH, null, null, 1))
        val transfer = WorkspaceBackup(db)
        val output = ByteArrayOutputStream(); transfer.write(output)
        val preview = transfer.preview(ByteArrayInputStream(output.toByteArray()))
        val corrupted = JSONObject(preview.json)
        corrupted.getJSONObject("tables").getJSONArray("tasks").getJSONObject(0).put("priority", "INVALID")
        assertTrue(runCatching { transfer.restore(preview.copy(json = corrupted.toString())) }.isFailure)
        assertEquals("Keep me", db.appDao().task("task")!!.title)
        corrupted.getJSONObject("tables").put("arbitrary_table", org.json.JSONArray())
        assertTrue(runCatching { transfer.preview(ByteArrayInputStream(corrupted.toString().toByteArray())) }.isFailure)
        assertEquals(1, db.appDao().observeTasks().first().size)
    }

    @Test fun projectRecoveryPreservesChangesMadeToItsTasks() = runBlocking {
        val repo = PersonalOSRepository(db)
        val recovery = RecoveryRepository(db)
        repo.saveProject(ProjectEntity("p", "Project", "Personal", ProjectStatus.ACTIVE, null, 1))
        repo.saveTask(TaskEntity("t", "Original", TaskStatus.INBOX, Priority.MEDIUM, null, "p", 1))
        repo.delete(CaptureKind.PROJECT, "p")
        val trashId = recovery.trash.first().single().id
        repo.saveTask(db.appDao().task("t")!!.copy(title = "Edited while project was deleted"))
        recovery.restore(trashId)
        assertEquals("Edited while project was deleted", db.appDao().task("t")!!.title)
        assertEquals("p", db.appDao().task("t")!!.projectId)
        assertNotNull(db.appDao().project("p"))
    }

    @Test fun trackerAndIndividualEntryRecoveryPreserveAllOwnedValues() = runBlocking {
        val repo = TrackerRepository(db)
        val recovery = RecoveryRepository(db)
        val id = repo.create("habits", "Daily")
        val checkbox = db.trackerDao().fields(id).first { it.kind == "CHECKBOX" }
        repo.saveEntry(TrackerEntryEntity("entry", id, "Today", "Notes", 1), mapOf(checkbox.id to "true"))
        repo.deleteEntry("entry")
        val entryTrash = recovery.trash.first().single().id
        repo.delete(id)
        assertTrue(runCatching { recovery.restore(entryTrash) }.isFailure)
        recovery.restore(recovery.trash.first().first { it.kind == "tracker" }.id)
        recovery.restore(entryTrash)
        assertEquals("Notes", db.trackerDao().entry("entry")!!.body)
        assertEquals("true", db.trackerDao().cells("entry").first { it.fieldId == checkbox.id }.value)
    }

    @Test fun removedRecordsAndNestedSectionsCanBeRecovered() = runBlocking {
        val repo = TemplateRepository(db)
        val recovery = RecoveryRepository(db)
        val id = repo.create("Budget", "", "finance")
        repo.saveRow(id, "budget", PageRow("category", "Books", mapOf("Monthly Budget" to "100")))
        repo.saveRow(id, "expenses", PageRow("expense", "Book", mapOf("Date" to "2026-10-12", "Amount" to "50", "Budget" to "[\"category\"]")))
        repo.deleteRow(id, "expenses", "expense")
        recovery.restore(recovery.trash.first().single().id)
        var document = TemplateDocument.parse(db.templatePageDao().page(id)!!.data)
        assertEquals("50", document.table("expenses").rows.single().values["Amount"])
        assertTrue("expense" in document.references(document.table("budget").rows.single(), "expenses"))
        val group = PageBlock("group", "GROUP", "Notes", children = listOf(PageBlock("child", "TEXT", "Keep this")))
        repo.change(id) { it.addBlock(group) }
        repo.change(id) { it.removeBlock("child") }
        recovery.restore(recovery.trash.first().single().id)
        document = TemplateDocument.parse(db.templatePageDao().page(id)!!.data)
        assertEquals("Keep this", document.blocks.first { it.id == "group" }.children.single().text)
    }

    @Test fun repeatingCompletionIsIdempotentAndSkipsAreRecoverable() = runBlocking {
        val repo = PersonalOSRepository(db)
        val today = LocalDate.now()
        repo.saveTask(TaskEntity("daily", "Read", TaskStatus.TODAY, Priority.MEDIUM, today.minusDays(3).toEpochDay(), null, 1, repeatDays = 1, reminderMinutes = 540))
        repo.toggleTask("daily", today)
        val successor = db.appDao().observeTasks().first().first { it.id != "daily" }
        assertEquals(today.plusDays(1).toEpochDay(), successor.dueEpochDay)
        assertEquals(540, successor.reminderMinutes)
        repo.toggleTask("daily", today); repo.toggleTask("daily", today)
        assertEquals(2, db.appDao().observeTasks().first().size)
        repo.skipTask(successor.id)
        assertEquals(2, db.appDao().observeTasks().first().size)
        val recovery = RecoveryRepository(db)
        recovery.restore(recovery.trash.first().single().id)
        assertEquals(3, db.appDao().observeTasks().first().size)
        repo.toggleTask(successor.id, today)
        assertEquals(3, db.appDao().observeTasks().first().size)
        repo.saveTask(TaskEntity("paused", "Pause me", TaskStatus.TODAY, Priority.MEDIUM, today.toEpochDay(), null, 1, repeatDays = 7, repeatPaused = true))
        repo.toggleTask("paused", today)
        assertEquals(4, db.appDao().observeTasks().first().size)
    }
}

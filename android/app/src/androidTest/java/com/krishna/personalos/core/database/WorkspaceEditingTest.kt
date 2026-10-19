package com.krishna.personalos.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krishna.personalos.core.repository.*
import com.krishna.personalos.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class WorkspaceEditingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var repo: PersonalOSRepository
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build(); repo = PersonalOSRepository(db) }
    @After fun close() = db.close()

    @Test fun editsPersistAndProjectDeletionKeepsItsTasks() = runBlocking {
        seedFixture()
        val task = db.appDao().task("task-focus")!!
        repo.saveTask(task.copy(title = "Changed", priority = Priority.LOW, status = TaskStatus.UPCOMING, dueEpochDay = 23000))
        repo.saveNote(db.appDao().note("note-training")!!.copy(body = "Changed body"))
        repo.saveProject(db.appDao().project("project-launch")!!.copy(status = ProjectStatus.PAUSED))
        repo.saveGoal(db.appDao().goal("goal-focus")!!.copy(progress = 90))
        assertEquals("Changed body", db.appDao().note("note-training")!!.body)
        assertEquals(90, db.appDao().goal("goal-focus")!!.progress)
        assertEquals(ProjectStatus.PAUSED, db.appDao().project("project-launch")!!.status)
        repo.delete(CaptureKind.PROJECT, "project-launch")
        assertNull(db.appDao().project("project-launch"))
        assertNull(db.appDao().task("task-focus")!!.projectId)
        assertEquals("Changed", db.appDao().task("task-focus")!!.title)
        assertFalse(db.appDao().observeRelations().first().any { it.sourceId == "project-launch" || it.targetId == "project-launch" })
        repo.delete(CaptureKind.NOTE, "note-training")
        repo.delete(CaptureKind.GOAL, "goal-focus")
        db.appDao().observeTasks().first().forEach { repo.delete(CaptureKind.TASK, it.id) }
        assertTrue(db.appDao().observeTasks().first().isEmpty())
        assertNull(db.appDao().note("note-training"))
        assertNull(db.appDao().goal("goal-focus"))
    }

    @Test fun invalidEditsDoNotReplaceSavedData() = runBlocking {
        seedFixture()
        val goal = db.appDao().goal("goal-focus")!!
        assertTrue(runCatching { repo.saveGoal(goal.copy(progress = 101)) }.isFailure)
        assertEquals(goal, db.appDao().goal(goal.id))
        val task = db.appDao().task("task-focus")!!
        assertTrue(runCatching { repo.saveTask(task.copy(projectId = "missing")) }.isFailure)
        assertEquals(task, db.appDao().task(task.id))
    }

    @Test fun noteChecklistTogglesPersistAsMarkdown() = runBlocking {
        val note = NoteEntity("checklist-note", "Trip", "Packing\n- [ ] Passport\n- [x] Charger", 1, 1)
        repo.saveNote(note)
        repo.toggleNoteChecklist(note.id, 1)
        assertEquals("Packing\n- [x] Passport\n- [x] Charger", db.appDao().note(note.id)!!.body)
    }

    @Test fun bundledDemoCleanupPreservesPersonalRecords() = runBlocking {
        seedFixture()
        db.appDao().insertTask(TaskEntity("my-task", "Keep me", TaskStatus.INBOX, Priority.MEDIUM, null, null, 2))
        db.appDao().insertNote(NoteEntity("my-note", "Keep note", "Mine", 2, 2))

        repo.removeBundledDemoData()

        assertNull(db.appDao().task("task-focus"))
        assertNull(db.appDao().note("note-training"))
        assertNotNull(db.appDao().task("my-task"))
        assertNotNull(db.appDao().note("my-note"))
    }

    @Test fun deletingImportedTemplateStaysDeletedAfterImportAndClearsData() = runBlocking {
        Assume.assumeTrue("templates.json" in context.assets.list("private-notion").orEmpty())
        val templates = TemplateRepository(db)
        templates.importBundled(context)
        val before = templates.pages.first()
        val page = before.first()
        templates.delete(page.id)
        templates.importBundled(context)
        assertEquals(before.map { it.id }.toSet() - page.id, templates.pages.first().map { it.id }.toSet())
        assertTrue(db.templatePageDao().page(page.id)!!.deleted)
        assertTrue(TemplateDocument.parse(db.templatePageDao().page(page.id)!!.data).tables.isEmpty())
    }

    @Test fun deletingCustomTemplateCleansOwnedRecordsOnly() = runBlocking {
        val trackers = TrackerRepository(db)
        val source = trackers.create("weekly", "First")
        val other = trackers.create("blank", "Keep")
        trackers.saveEntry(TrackerEntryEntity("entry", source, "Read", createdAt = 1), emptyMap())
        trackers.delete(source)
        assertNull(db.trackerDao().tracker(source))
        assertTrue(db.trackerDao().fields(source).isEmpty())
        assertTrue(db.trackerDao().entries(source).isEmpty())
        assertTrue(db.trackerDao().cells("entry").isEmpty())
        assertNotNull(db.trackerDao().tracker(other))
    }

    private suspend fun seedFixture() {
        val now = 1L
        db.appDao().insertProject(ProjectEntity("project-launch", "Project", "Personal", ProjectStatus.ACTIVE, null, now))
        db.appDao().insertGoal(GoalEntity("goal-focus", "Goal", "Quarter", 10, now))
        db.appDao().insertTask(TaskEntity("task-focus", "Task", TaskStatus.TODAY, Priority.HIGH, null, "project-launch", now))
        db.appDao().insertNote(NoteEntity("note-training", "Note", "Body", now, now))
        db.appDao().insertRelations(listOf(EntityRelationEntity("relation-1", "task-focus", "project-launch", RelationType.BELONGS_TO, now)))
    }
}

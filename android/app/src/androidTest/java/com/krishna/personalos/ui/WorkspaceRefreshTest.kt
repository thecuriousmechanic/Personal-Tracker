package com.krishna.personalos.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.export.WorkspaceBackup
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.*
import com.krishna.personalos.ui.theme.PersonalOSTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate

class WorkspaceRefreshTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var templates: TemplateRepository
    private lateinit var trackers: TrackerRepository
    private lateinit var workspace: PersonalOSRepository
    private lateinit var recovery: RecoveryRepository
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        templates = TemplateRepository(db); trackers = TrackerRepository(db); workspace = PersonalOSRepository(db); recovery = RecoveryRepository(db)
        val routine = templates.create("Morning rhythm", "Small steps, every day", "routine")
        templates.create("Money journal", "Keep your spending in view", "finance")
        templates.create("Reading shelf", "Books, ideas, and what comes next", "reading")
        recovery.pin(routine, true)
        workspace.saveTask(TaskEntity("today", "Read a chapter", TaskStatus.TODAY, Priority.MEDIUM, LocalDate.now().toEpochDay(), null, 1))
        workspace.saveTask(TaskEntity("today-2", "Plan tomorrow", TaskStatus.TODAY, Priority.LOW, LocalDate.now().toEpochDay(), null, 2))
    }
    @After fun close() { db.close(); TemplateDocumentCache.clear() }

    private fun showApp(dark: Boolean = false, fontScale: Float = 1f) {
        val trackerActions = TrackerActions(trackers::create, trackers::saveTracker, trackers::saveField, trackers::saveEntry, trackers::toggleCheckbox, trackers::archiveEntry, trackers::deleteEntry, trackers::deleteEntries, trackers::duplicate, trackers::delete)
        val templateActions = TemplateActions(templates::create, templates::change, templates::rename, templates::archive, templates::duplicate, templates::saveRow, templates::deleteRow, templates::deleteRows, templates::delete, importDocument = templates::importDocument)
        val workspaceActions = WorkspaceActions(workspace::saveTask, workspace::saveNote, workspace::saveProject, workspace::saveGoal, workspace::delete, workspace::toggleNoteChecklist, workspace::skipTask)
        compose.setContent {
            val state by workspace.workspace.collectAsState(WorkspaceSnapshot())
            val trackerState by trackers.workspace.collectAsState(TrackerWorkspace())
            val pages by templates.pages.collectAsState(emptyList())
            val preferences by recovery.preferences.collectAsState(emptyList())
            val trash by recovery.trash.collectAsState(emptyList())
            var darkMode by remember { mutableStateOf(dark) }
            val scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            PersonalOSTheme(darkMode) {
                PersonalOSApp(trackerState, trackerActions, state, workspaceActions, pages, templateActions, null,
                    onToggleTask = { scope.launch { workspace.toggleTask(it) } }, onScheduleToday = {}, onCapture = { _, _ -> },
                    darkMode = darkMode, onDarkMode = { darkMode = it },
                    pinnedIds = preferences.filter { it.key.startsWith("pin:") && it.value == "true" }.map { it.key.removePrefix("pin:") }.toSet(),
                    onPin = { id, pin -> scope.launch { recovery.pin(id, pin) } },
                    trash = trash, recovery = recovery, backup = WorkspaceBackup(db), onRestoreBackup = { WorkspaceBackup(db).restore(it) })
            }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("PINNED").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun refreshedHomePinsPagesAndCreatesBudgetPages() {
        showApp()
        screenshot("refresh-home-light")
        compose.onNodeWithTag("template-home").performScrollToNode(hasTestTag("new-template"))
        compose.onNodeWithTag("new-template").performClick()
        compose.onNodeWithText("Page name").performTextInput("Travel budget")
        compose.onNodeWithText("Budget & spending", substring = false).performScrollTo().performClick()
        screenshot("refresh-new-page")
        compose.onNodeWithText("Create page", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("template-page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Travel budget", substring = false).assertIsDisplayed()
        screenshot("refresh-budget-page")
        val saved = runBlocking { templates.pages.first().first { it.title == "Travel budget" } }
        assertEquals("finance", saved.kind)
        assertEquals(setOf("budget", "expenses", "incomes"), TemplateDocument.parse(saved.data).tables.map { it.id }.toSet())
    }

    @Test fun deletedPageCanBeRestoredFromSettingsTrash() {
        showApp()
        compose.onNodeWithTag("template-home").performScrollToNode(hasText("Morning rhythm", substring = false))
        compose.onNodeWithText("Morning rhythm", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("template-page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Template actions").performClick()
        compose.onNodeWithText("Move to Trash", substring = false).performClick()
        compose.onNodeWithText("Move to Trash", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("template-home").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNode(hasText("Settings", substring = false) and hasClickAction()).performClick()
        screenshot("refresh-settings")
        compose.onNodeWithText("Trash", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Morning rhythm", substring = false).assertIsDisplayed()
        screenshot("refresh-trash")
        compose.onNodeWithText("Restore", substring = false).performClick()
        compose.waitUntil(10_000) { runBlocking { recovery.trash.first().isEmpty() } }
        compose.onNode(hasText("Home", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithTag("template-home").performScrollToNode(hasText("Morning rhythm", substring = false))
        compose.onNodeWithText("Morning rhythm", substring = false).assertIsDisplayed()
    }

    @Test fun recurringTaskEditorPersistsWeeklyRepeatAndPause() {
        showApp()
        compose.onNode(hasText("Plan", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithText("Read a chapter", substring = false).performClick()
        compose.onNodeWithText("Weekly", substring = false).performScrollTo().performClick()
        screenshot("refresh-task-editor")
        compose.onNodeWithText("Save task", substring = false).performClick()
        compose.waitUntil(10_000) { runBlocking { db.appDao().task("today")!!.repeatDays == 7 } }
        compose.onNodeWithText("Read a chapter", substring = false).performClick()
        compose.onNodeWithText("Pause repeat and reminders", substring = false).performScrollTo()
        compose.onNodeWithContentDescription("Pause repeat and reminders").performClick()
        compose.onNodeWithText("Save task", substring = false).performClick()
        compose.waitUntil(10_000) { runBlocking { db.appDao().task("today")!!.repeatPaused } }
    }

    @Test fun darkAppearanceAndLargeTextKeepNavigationAccessible() {
        showApp(dark = true, fontScale = 1.3f)
        screenshot("refresh-home-dark")
        compose.onNode(hasText("Settings", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithContentDescription("Dark appearance").assertIsOn()
        compose.onNodeWithContentDescription("Dark appearance").performClick()
        compose.onNodeWithContentDescription("Dark appearance").assertIsOff()
        compose.onNodeWithText("Back up workspace", substring = false).performScrollTo().assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.getExternalFilesDir(null)!!.absolutePath, "tracker-qa").apply { mkdirs() }
        val node = if (compose.onAllNodesWithTag("workspace-editor").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("workspace-editor") else compose.onRoot()
        node.captureToImage().asAndroidBitmap().let { bitmap -> File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
}

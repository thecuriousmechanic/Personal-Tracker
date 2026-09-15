package com.krishna.personalos.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.model.TrackerActions
import com.krishna.personalos.core.model.TrackerWorkspace
import com.krishna.personalos.core.repository.TrackerRepository
import com.krishna.personalos.ui.theme.PersonalOSTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

class TrackerScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AppDatabase
    private lateinit var repository: TrackerRepository
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TrackerRepository(database)
    }
    @After fun tearDown() { database.close() }

    @Test fun createEditAndCheckOffWeeklyRoutine() {
        val actions = TrackerActions(repository::create, repository::saveTracker, repository::saveField, repository::saveEntry, repository::toggleCheckbox, repository::archiveEntry, repository::deleteEntry, repository::deleteEntries, repository::duplicate, repository::delete)
        compose.setContent {
            val workspace by repository.workspace.collectAsState(TrackerWorkspace())
            PersonalOSTheme { Surface(color = com.krishna.personalos.ui.theme.Ink) { TrackerScreen(workspace, actions, PaddingValues()) } }
        }
        compose.onNodeWithText("Create a tracker").performClick()
        screenshot("templates")
        compose.onNodeWithText("Weekly routine").performClick()
        compose.onNodeWithText("Tracker name").performTextReplacement("Morning rhythm")
        compose.onNode(hasText("Create tracker") and hasClickAction()).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Customize").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Entry", substring = false).performClick()
        compose.onNodeWithText("Entry name").performTextInput("Read a chapter")
        compose.onNodeWithText("Week of").performTextInput("2026-09-07")
        compose.onNodeWithText("Save entry").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Read a chapter").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Table view").performClick()
        compose.onNodeWithTag("tracker-table").performSemanticsAction(SemanticsActions.ScrollBy) { it(280f * context.resources.displayMetrics.density, 0f) }
        screenshot("table-scrolled")
        compose.onNodeWithContentDescription("Read a chapter, Mon").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runBlocking { repository.workspace.first().cells.any { it.value == "true" } }
        }
        compose.onNodeWithContentDescription("Read a chapter, Mon").assertIsOn()
        screenshot("weekly-table")
        compose.onNodeWithText("List view").performClick()
        compose.onNodeWithText("Read a chapter").performTouchInput { longClick() }
        compose.onNodeWithTag("select-all-entries").assertIsDisplayed().performClick()
        compose.onNodeWithText("Clear").performClick()
        compose.onNodeWithText("Read a chapter").performClick()
        compose.onNodeWithContentDescription("Note text 1").performScrollTo().performTextInput("A quiet start to the day.")
        compose.onNodeWithText("Checklist").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Checklist item 2").performScrollTo().performTextInput("Pack charger")
        compose.onNodeWithText("Save entry").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { repository.workspace.first().entries.any { it.body == "A quiet start to the day.\n- [ ] Pack charger" } } }
        compose.onNodeWithText("Customize").performClick()
        compose.onNodeWithText("+ Add a field").performScrollTo().performClick()
        compose.onNodeWithText("Field name").performTextInput("Where")
        compose.onNodeWithText("Save field").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Customize tracker").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Cancel").performClick()
        screenshot("weekly-list")
        runBlocking { assertTrue(repository.workspace.first().fields.any { it.name == "Where" }) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.getExternalFilesDir(null)!!.absolutePath, "tracker-qa").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}

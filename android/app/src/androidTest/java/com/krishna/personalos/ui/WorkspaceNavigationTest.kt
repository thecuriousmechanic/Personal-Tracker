package com.krishna.personalos.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.krishna.personalos.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import java.io.File
import androidx.compose.ui.semantics.SemanticsActions
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.model.TemplateDocument
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class WorkspaceNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun keepTestActivityVisible() {
        org.junit.Assume.assumeTrue("Legacy private-template suite needs optional privateAssetsDir", "templates.json" in InstrumentationRegistry.getInstrumentation().targetContext.assets.list("private-notion").orEmpty())
        compose.activityRule.scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    @Test fun rapidChecklistTapsAreSavedWhenLeavingThePage() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("MY TRACKER").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("MY TRACKER").performClick()
        val fields = listOf("Sleep 7-8 hours", "Eat healthy meals", "Social media ≤ 90min")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun saved() = runBlocking {
            val page = AppDatabase.get(context).templatePageDao().page("b9dc8b8deafe83d08b2101df22c0543f")!!
            TemplateDocument.parse(page.data).table("daily").rows.firstOrNull { !it.archived && it.date() == LocalDate.now() }?.values.orEmpty()
        }
        val before = saved()
        val taps = fields.map { field -> compose.onNodeWithContentDescription("Daily quest: $field").fetchSemanticsNode().config[SemanticsActions.OnClick].action!! }
        // Deliver a burst before the next frame or database acknowledgement.
        compose.runOnIdle { taps.forEach { it() } }
        compose.onNodeWithContentDescription("Back to templates").performClick()
        compose.waitUntil(10_000) { val values = saved(); fields.all { (values[it] == "true") != (before[it] == "true") } }
        compose.onNodeWithText("MY TRACKER").performClick()
        fields.forEach { field -> compose.onNodeWithContentDescription("Daily quest: $field").performClick() }
        compose.waitUntil(10_000) { val values = saved(); fields.all { (values[it] == "true") == (before[it] == "true") } }
    }

    @Test fun dailyAndWeeklyProgressCanBeEditedAndDeleted() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("MY TRACKER").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("MY TRACKER").performClick()
        compose.onNodeWithTag("daily-quest-edit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("template-entry-title").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("template-entry-title").performTextReplacement("QA weekly edit")
        compose.onNodeWithText("Save entry", substring = false).performClick()

        val today = LocalDate.now().toString()
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("Weekly progress", substring = false))
        compose.onNodeWithTag("template-page").performScrollToNode(hasText(today, substring = false))
        compose.onNodeWithText(today, substring = false).performClick()
        compose.onNode(hasSetTextAction() and hasText("QA weekly edit")).assertIsDisplayed()
        compose.onNodeWithText("Delete entry", substring = false).performClick()
        compose.onNodeWithText("Keep it", substring = false).performClick()
        compose.onNode(hasSetTextAction() and hasText("QA weekly edit")).assertIsDisplayed()
        compose.onNodeWithText("Delete entry", substring = false).performClick()
        compose.onNodeWithText("Delete permanently", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("workspace-editor").fetchSemanticsNodes().isEmpty() }
    }

    @Test fun appearanceToggleSwitchesToDarkModeAndBack() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Week Plan").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithContentDescription("Use dark mode").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Use dark mode").performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Use light mode").fetchSemanticsNodes().isNotEmpty() }
        screenshot("home-dark")
        compose.onNodeWithContentDescription("Use light mode").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Use dark mode").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun noteChecklistCanBeCreatedAndToggledFromLibrary() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Week Plan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Library", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithText("New note", substring = false).performClick()
        compose.onNodeWithText("Title", substring = false).performTextInput("QA checklist note")
        compose.onNodeWithText("Checklist", substring = false).performClick()
        compose.onNodeWithContentDescription("Checklist item 1").performTextInput("Passport")
        compose.onNodeWithText("Save note", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Complete Passport").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Complete Passport").performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.waitUntil(10_000) {
            runBlocking { AppDatabase.get(context).appDao().observeNotes().first().firstOrNull { it.title == "QA checklist note" }?.body?.contains("- [x] Passport") == true }
        }
        screenshot("note-checklist")
        compose.onNodeWithText("QA checklist note", substring = false).performClick()
        compose.onNodeWithText("Delete note", substring = false).performClick()
        compose.onNodeWithText("Delete permanently").performClick()
    }

    @Test fun homeOpensEachImportedTemplateDirectly() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Week Plan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Your templates").assertIsDisplayed()
        compose.onNodeWithText("Apps", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Habits", substring = false).assertDoesNotExist()
        screenshot("home-templates")
        listOf("Week Plan" to "week-plan", "MY TRACKER" to "personal-tracker", "Budget & Finance Tracker" to "finance-tracker").forEach { (title, name) ->
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.onNodeWithContentDescription("Back to templates").assertIsDisplayed()
            compose.onNodeWithText(title).assertIsDisplayed()
            screenshot(name)
            if (name == "week-plan") {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val hasBundledRows = runBlocking {
                    val page = AppDatabase.get(context).templatePageDao().observe().first().first { it.title == "Week Plan" }
                    TemplateDocument.parse(page.data).tables.any { it.rows.any { row -> row.id.startsWith("routine-") } }
                }
                org.junit.Assert.assertFalse(hasBundledRows)
            }
            if (name == "personal-tracker") {
                compose.onNodeWithContentDescription("Daily quest: Sleep 7-8 hours").performClick()
                compose.waitUntil(5_000) { compose.onAllNodesWithText("10% complete").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithContentDescription("Daily quest: Sleep 7-8 hours").performClick()
            }
            if (name == "finance-tracker") {
                compose.onNodeWithText("+ Expense").performClick()
                compose.onNodeWithText("Name", substring = false).performTextInput("QA purchase")
                compose.onNodeWithText("Amount", substring = false).performTextInput("12.50")
                screenshot("expense-editor")
                compose.onNodeWithText("Save entry").assertIsDisplayed().performClick()
                compose.waitUntil(5_000) { compose.onAllNodesWithText("$12.50").fetchSemanticsNodes().isNotEmpty() }
                screenshot("finance-updated")
            }
            compose.onNodeWithContentDescription("Back to templates").performClick()
            compose.onNodeWithText("Your templates").assertIsDisplayed()
        }
        compose.onNode(hasText("Plan", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithText("Make room for what matters").assertIsDisplayed()
        screenshot("plan-paper")
        compose.onNode(hasText("Library", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithText("Everything worth keeping").assertIsDisplayed()
        screenshot("library-paper")
        compose.onNodeWithContentDescription("Add", substring = false).performClick()
        compose.onNode(hasText("New note", substring = false) and hasAnyAncestor(hasTestTag("workspace-editor"))).assertIsDisplayed()
        screenshot("capture-paper")
    }

    @Test fun editAndDeleteEveryWorkspaceItemFromVisibleControls() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Week Plan").fetchSemanticsNodes().isNotEmpty() }
        listOf("note", "project", "goal", "task").forEach { kind ->
            compose.onNode(hasText(if (kind == "task") "Plan" else "Library", substring = false) and hasClickAction()).performClick()
            if (kind != "task") compose.onNodeWithText("${kind.replaceFirstChar(Char::uppercase)}s", substring = false).performClick()
            compose.onNodeWithText("New $kind", substring = false).performClick()
            compose.onNodeWithText("Title", substring = false).performClick().performTextInput("QA $kind")
            if (kind == "note") compose.onNodeWithContentDescription("Note text 1").performTextInput("Editable body")
            if (kind == "goal") compose.onNodeWithText("Progress (%)").performTextReplacement("65")
            compose.onNodeWithText("Save $kind", substring = false).assertIsDisplayed().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("QA $kind", substring = false).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("QA $kind", substring = false).performClick()
            compose.onNodeWithText("Title", substring = false).performTextReplacement("Updated $kind")
            screenshot("edit-$kind")
            compose.onNodeWithText("Save $kind", substring = false).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Updated $kind", substring = false).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Updated $kind", substring = false).performClick()
            compose.onNodeWithText("Delete $kind", substring = false).assertIsDisplayed().performClick()
            compose.onNodeWithText("Keep it").performClick()
            compose.onNodeWithText("Title", substring = false).assertTextContains("Updated $kind")
            compose.onNodeWithText("Delete $kind", substring = false).performClick()
            compose.onNodeWithText("Delete permanently").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("workspace-editor").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Updated $kind", substring = false).assertDoesNotExist()
        }
    }

    @Test fun templateCanBeRenamedAndDeletedThroughHomeActions() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Week Plan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Week Plan").performClick()
        compose.onNodeWithContentDescription("Template actions").performClick()
        compose.onNodeWithText("Duplicate template").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Week Plan copy").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Back to templates").performClick()
        compose.onNodeWithTag("template-home").performScrollToNode(hasContentDescription("Actions for Week Plan copy"))
        compose.onNodeWithContentDescription("Actions for Week Plan copy").performClick()
        compose.onNodeWithText("Edit template").performClick()
        compose.onNodeWithText("Rename", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Name", substring = false).performTextReplacement("QA renamed template")
        compose.onNodeWithText("Save template").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("QA renamed template").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Back to templates").performClick()
        compose.onNodeWithTag("template-home").performScrollToNode(hasContentDescription("Actions for QA renamed template"))
        compose.onNodeWithContentDescription("Actions for QA renamed template").performClick()
        compose.onNodeWithText("Delete template").performClick()
        compose.onNodeWithText("Delete permanently").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("QA renamed template").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Week Plan").assertExists()
    }

    @Test fun templateSectionsCanBeAddedEditedAndDeleted() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("MY TRACKER", substring = false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("template-home").performScrollToNode(hasText("Syllabus", substring = false))
        compose.onNodeWithText("Syllabus", substring = false).performClick()
        compose.onNodeWithText("Edit", substring = false).performClick()
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("Add section", substring = false))
        compose.onNodeWithText("Add section", substring = false).performClick()
        compose.onNodeWithText("Section content", substring = false).performTextInput("QA section")
        compose.onNodeWithText("Add to page", substring = false).performClick()
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("QA section", substring = false))
        compose.onNodeWithContentDescription("Edit QA section").performClick()
        compose.onNodeWithText("Section content", substring = false).performTextReplacement("QA section updated")
        compose.onNodeWithText("Save changes", substring = false).performClick()
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("QA section updated", substring = false))
        compose.onNodeWithContentDescription("Section actions for QA section updated").performClick()
        compose.onNodeWithText("Delete section", substring = false).performClick()
        compose.onNodeWithText("Delete permanently", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("QA section updated", substring = false).fetchSemanticsNodes().isEmpty() }
    }

    @Test fun starterBoardShowsUndatedEntriesAndCalendarCanCreateRecords() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("new-template").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("new-template").performScrollTo().performClick()
        compose.onNodeWithText("Template name").performTextInput("QA enhanced workspace")
        compose.onNodeWithText("Project board", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Create template", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("QA enhanced workspace", substring = false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("+ Add entry", substring = false))
        compose.onNodeWithText("+ Add entry", substring = false).performClick()
        compose.onNodeWithTag("template-entry-title").performTextInput("Unscheduled project")
        compose.onNodeWithText("Not started", substring = false).performClick()
        compose.onNodeWithText("Due date", substring = false).performTextReplacement("")
        compose.onNodeWithText("Save entry", substring = false).performClick()
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("Unscheduled project", substring = false))
        compose.onNodeWithText("Unscheduled project", substring = false).assertIsDisplayed()
        screenshot("enhanced-board")
        compose.onNodeWithTag("template-page").performScrollToNode(hasTestTag("add-database"))
        compose.onNodeWithTag("add-database").performClick()
        compose.onNodeWithText("Database name").performTextInput("Dates")
        compose.onNodeWithText("Calendar", substring = false).performClick()
        compose.onNodeWithText("Create database", substring = false).performScrollTo().performClick()
        val today = LocalDate.now().toString()
        compose.onNodeWithTag("template-page").performScrollToNode(hasContentDescription("Calendar $today, 0 records"))
        compose.onNodeWithContentDescription("Calendar $today, 0 records").performClick()
        compose.onNodeWithTag("template-entry-title").performTextInput("Calendar appointment")
        compose.onNodeWithText("Save entry", substring = false).performClick()
        compose.onNodeWithTag("template-page").performScrollToNode(hasText("Calendar appointment", substring = false))
        compose.onNodeWithText("Calendar appointment", substring = false).assertIsDisplayed()
        screenshot("enhanced-calendar")
        compose.onNodeWithContentDescription("Template actions").performClick()
        compose.onNodeWithText("Delete template", substring = false).performClick()
        compose.onNodeWithText("Delete permanently", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("template-home").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun homeQuickCreateOpensTheEditorAndShowsTheSavedNote() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("new-template").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Add", substring = false).performClick()
        compose.onNodeWithText("Create something", substring = false).assertIsDisplayed()
        compose.onNodeWithText("New note", substring = false).performClick()
        compose.onNodeWithText("Title", substring = false).performTextInput("QA quick note")
        compose.onNodeWithText("Save note", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("QA quick note", substring = false).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("QA quick note", substring = false).performClick()
        compose.onNodeWithText("Delete note", substring = false).performClick()
        compose.onNodeWithText("Delete permanently", substring = false).performClick()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: context.getExternalFilesDir(null)!!.absolutePath, "tracker-qa").apply { mkdirs() }
        val node = when {
            compose.onAllNodesWithTag("workspace-editor").fetchSemanticsNodes().isNotEmpty() -> compose.onNodeWithTag("workspace-editor")
            compose.onAllNodesWithTag("capture-sheet").fetchSemanticsNodes().isNotEmpty() -> compose.onNodeWithTag("capture-sheet")
            else -> compose.onRoot()
        }
        node.captureToImage().asAndroidBitmap().let { image -> File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
}

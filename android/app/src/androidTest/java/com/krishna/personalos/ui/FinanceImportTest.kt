package com.krishna.personalos.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.TemplateRepository
import com.krishna.personalos.ui.theme.PersonalOSTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class FinanceImportTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun importIsCleanRepeatableAndPreservesUserRecords() = runBlocking {
        org.junit.Assume.assumeTrue("Optional private bundle required", "templates.json" in context.assets.list("private-notion").orEmpty())
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TemplateRepository(db)
            val id = repo.importFinance(context)
            fun document() = runBlocking { TemplateDocument.parse(db.templatePageDao().page(id)!!.data) }
            val imported = document()
            assertEquals(8, imported.table("budget").rows.size)
            assertTrue(imported.table("budget").rows.all { it.values.isEmpty() })
            assertTrue(imported.tables.filter { it.id != "budget" }.all { it.rows.isEmpty() })
            val category = imported.table("budget").rows.first()
            repo.saveRow(id, "budget", category.copy(values = mapOf("Monthly Budget" to "100")))
            repo.saveRow(id, "expenses", PageRow("real", "My expense", mapOf("Amount" to "125", "Date" to "2026-09-16", "Budget" to "[\"${category.id}\"]")))
            repo.archive(id, true)
            assertEquals(id, repo.importFinance(context))
            assertFalse(db.templatePageDao().page(id)!!.archived)
            assertEquals(8, document().table("budget").rows.size)
            assertEquals("My expense", document().table("expenses").rows.single().title)
            assertEquals("125%", document().computed(document().table("budget"), document().table("budget").rows.first(), PageField("Utilization", "", "FORMULA"), YearMonth.of(2026, 9)))
            repo.importBundled(context)
            assertEquals(8, document().table("budget").rows.size)
            assertEquals("real", document().table("expenses").rows.single().id)
            repo.delete(id)
            assertTrue(db.templatePageDao().page(id)!!.deleted)
            repo.importFinance(context)
            assertFalse(db.templatePageDao().page(id)!!.deleted)
            assertTrue(document().table("expenses").rows.isEmpty())
        } finally { db.close() }
    }

    @Test fun ringsExposeAccurateProgressIncludingOverspend() {
        val progress = mutableFloatStateOf(0f)
        compose.setContent { PersonalOSTheme { Column {
            ProgressRing(progress.floatValue, Color.Green, "Test progress")
            ProgressRing(1.25f, Color.Red, "Budget usage", valueText = "125%", compact = true)
            ProgressRing(Float.NaN, Color.Green, "Empty progress")
        } } }
        compose.onNodeWithContentDescription("Test progress").assertRangeEquals(0f)
        compose.runOnIdle { progress.floatValue = .5f }
        compose.onNodeWithContentDescription("Test progress").assertRangeEquals(.5f)
        compose.runOnIdle { progress.floatValue = 1f }
        compose.onNodeWithContentDescription("Test progress").assertRangeEquals(1f)
        compose.onNodeWithContentDescription("Budget usage").assertRangeEquals(1f)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "125%"))
        compose.onNodeWithContentDescription("Empty progress").assertRangeEquals(0f)
    }

    @Test fun financePageShowsCircularBudgetsAndEditableCategories() {
        org.junit.Assume.assumeTrue("Optional private bundle required", "templates.json" in context.assets.list("private-notion").orEmpty())
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TemplateRepository(db)
            val id = runBlocking { repo.importFinance(context) }
            val actions = TemplateActions(repo::create, repo::change, repo::rename, repo::archive, repo::duplicate, repo::saveRow, repo::deleteRow, repo::deleteRows, repo::delete)
            compose.setContent { val pages by repo.pages.collectAsState(emptyList())
                PersonalOSTheme { pages.firstOrNull { it.id == id }?.let { TemplatePageScreen(it, actions, PaddingValues(), {}, {}) } }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("template-page").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("template-page").performScrollToNode(hasContentDescription("Activities progress"))
            compose.onNodeWithContentDescription("Activities progress").assertRangeEquals(0f)
            compose.onNodeWithText("Activities").performClick()
            compose.onNode(hasText("Monthly Budget") and hasSetTextAction()).assertExists()
        } finally { db.close() }
    }
}

private fun SemanticsNodeInteraction.assertRangeEquals(value: Float) = assert(
    SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(value, 0f..1f)))

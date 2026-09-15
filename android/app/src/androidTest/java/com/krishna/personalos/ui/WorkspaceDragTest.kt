package com.krishna.personalos.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.TemplateRepository
import com.krishna.personalos.ui.theme.PersonalOSTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class WorkspaceDragTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun drag(from: Offset, to: Offset) {
        compose.onRoot().performTouchInput {
            down(from)
            advanceEventTime(700)
            moveBy(Offset(0f, 2f))
            moveTo(to, delayMillis = 300)
            up()
        }
    }

    @Test fun noteLinesCanBeDraggedWithoutChangingCompletion() {
        val body = mutableStateOf("- [x] First\n- [ ] Second")
        compose.setContent { PersonalOSTheme { Surface { Column { NoteBlockEditor(body.value) { body.value = it } } } } }
        val start = compose.onNodeWithContentDescription("Drag note line 1").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithTag("drop-target-1").fetchSemanticsNode().boundsInRoot
        drag(start, Offset(end.center.x, end.bottom - 3f))
        compose.waitUntil { body.value == "- [ ] Second\n- [x] First" }
        compose.onNodeWithText("Reset checks").performClick()
        assertEquals("- [ ] Second\n- [ ] First", body.value)
    }

    @Test fun boardCardsCanBeDraggedIntoAnotherStatusColumn() {
        val table = mutableStateOf(PageTable("board", "Projects", listOf(PageField("status", "Status", "CHOICE", listOf("Open", "Done"))), listOf(PageRow("card", "Ship feature", mapOf("status" to "Open")))))
        compose.setContent { PersonalOSTheme { Surface { Column { WorkspaceBoard(table.value, false, {}) { row, field, status ->
            table.value = TemplateDocument(emptyList(), listOf(table.value)).setBoardStatus("board", row, field, status).table("board")
        } } } } }
        val start = compose.onNodeWithContentDescription("Drag card Ship feature").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithTag("drop-target-column:Done").fetchSemanticsNode().boundsInRoot.center
        drag(start, end)
        compose.waitForIdle()
        assertEquals("Card drop from $start to $end", "Done", table.value.rows.single().values["status"])
    }

    @Test fun draggedPageSectionsPersistInTheDatabase() {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TemplateRepository(db)
            val document = TemplateDocument(listOf(PageBlock("first", "TEXT", "First"), PageBlock("second", "TEXT", "Second"), PageBlock("group", "GROUP", "Group")), emptyList())
            runBlocking { db.templatePageDao().importPage(TemplatePageEntity("drag-page", "Drag page", "", "personal", "", "", "2026-09-15", document.toJson())) }
            val actions = TemplateActions(repo::create, repo::change, repo::rename, repo::archive, repo::duplicate, repo::saveRow, repo::deleteRow, repo::deleteRows, repo::delete)
            compose.setContent {
                val pages by repo.pages.collectAsState(emptyList())
                PersonalOSTheme { pages.firstOrNull()?.let { TemplatePageScreen(it, actions, PaddingValues(), {}, {}, initiallyEditing = true) } }
            }
            compose.waitUntil { compose.onAllNodesWithContentDescription("Drag section First").fetchSemanticsNodes().isNotEmpty() }
            val start = compose.onNodeWithContentDescription("Drag section First").fetchSemanticsNode().boundsInRoot.center
            val end = compose.onNodeWithTag("drop-target-second").fetchSemanticsNode().boundsInRoot
            drag(start, Offset(end.center.x, end.bottom - 3f))
            compose.waitUntil(10_000) { runBlocking { TemplateDocument.parse(db.templatePageDao().page("drag-page")!!.data).blocks.map { it.id } == listOf("second", "first", "group") } }
            compose.onNodeWithTag("template-page").performScrollToNode(hasTestTag("drop-target-inside:group"))
            val source = compose.onNodeWithContentDescription("Drag section First").fetchSemanticsNode().boundsInRoot.center
            val group = compose.onNodeWithTag("drop-target-inside:group").fetchSemanticsNode().boundsInRoot.center
            drag(source, group)
            compose.waitUntil(10_000) { runBlocking { TemplateDocument.parse(db.templatePageDao().page("drag-page")!!.data).blocks.last().children.map { it.id } == listOf("first") } }
        } finally { db.close() }
    }
}

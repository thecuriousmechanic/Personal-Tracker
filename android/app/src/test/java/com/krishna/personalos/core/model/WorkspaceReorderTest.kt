package com.krishna.personalos.core.model

import org.junit.Assert.*
import org.junit.Test

class WorkspaceReorderTest {
    private val first = PageBlock("first", "TEXT", "First")
    private val second = PageBlock("second", "CHECK", "Second", checked = true)
    private val child = PageBlock("child", "TEXT", "Nested")
    private val group = PageBlock("group", "GROUP", "Group", children = listOf(child))
    private val doc = TemplateDocument(listOf(first, second, group), emptyList())

    @Test fun reorderKeepsIdentityContentAndCheckState() {
        val moved = doc.relocateBlock("first", "second", BlockPlacement.AFTER)
        assertEquals(listOf(second, first, group), moved.blocks)
        assertEquals(doc, moved.relocateBlock("first", "second", BlockPlacement.BEFORE))
    }

    @Test fun blocksCanBeNestedAndMovedBackToPageRoot() {
        val nested = doc.relocateBlock("second", "group", BlockPlacement.INSIDE)
        assertEquals(listOf(child, second), nested.blocks.last().children)
        assertEquals(listOf(first, group.copy(children = listOf(child, second))), nested.blocks)
        assertEquals(doc, nested.relocateBlock("second", "group", BlockPlacement.BEFORE))
    }

    @Test fun illegalDropsNeverLoseBlocksOrCreateCycles() {
        assertEquals(doc, doc.relocateBlock("group", "child", BlockPlacement.INSIDE))
        assertEquals(doc, doc.relocateBlock("group", "child", BlockPlacement.AFTER))
        assertEquals(doc, doc.relocateBlock("first", "first", BlockPlacement.BEFORE))
        assertEquals(doc, doc.relocateBlock("first", "missing", BlockPlacement.AFTER))
        assertEquals(doc, doc.relocateBlock("first", "second", BlockPlacement.INSIDE))
    }

    @Test fun checklistReorderAndBulkCompletionPreserveMarkdownAndProse() {
        val text = "Intro\n- [ ] Passport\n* [x] Charger"
        assertEquals("Intro\n* [x] Charger\n- [ ] Passport", text.moveNoteLine(1, 2, true))
        assertEquals("Intro\n- [x] Passport\n* [x] Charger", text.setChecklistCompletion(true))
        assertEquals("Intro\n- [ ] Passport\n* [ ] Charger", text.setChecklistCompletion(false))
        assertEquals(text, text.moveNoteLine(0, 0, true))
    }

    @Test fun boardDropChangesOnlyTheChosenStatus() {
        val table = PageTable("board", "Projects", listOf(PageField("status", "Status", "CHOICE", listOf("Open", "Done"))), listOf(
            PageRow("one", "First", mapOf("status" to "Open", "notes" to "Keep")),
            PageRow("two", "Second", mapOf("status" to "Open")),
        ))
        val original = TemplateDocument(emptyList(), listOf(table))
        val moved = original.setBoardStatus("board", "one", "status", "Done")
        assertEquals(mapOf("status" to "Done", "notes" to "Keep"), moved.table("board").rows.first().values)
        assertEquals(table.rows.last(), moved.table("board").rows.last())
        assertEquals(original, original.setBoardStatus("board", "one", "status", "Unavailable"))
    }
}

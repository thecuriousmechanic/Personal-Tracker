package com.krishna.personalos.core.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TemplateToolsTest {
    private val table = PageTable("work", "Work", listOf(PageField("due", "Due", "DATE"), PageField("status", "Status", "CHOICE", listOf("Open", "Done"))), listOf(
        PageRow("one", "Write proposal", mapOf("due" to "2026-09-18", "status" to "Open", "notes" to "Client review")),
        PageRow("two", "Book meeting", mapOf("due" to "2026-09-16", "status" to "Done")),
        PageRow("three", "Unscheduled", emptyMap()),
        PageRow("archive", "Archived proposal", mapOf("status" to "Open"), archived = true),
    ))

    @Test fun recordSearchCombinesWordsAndRespectsArchiveAndChoice() {
        assertEquals(listOf("one"), table.records("proposal client", false, RecordSort.ADDED, "status", "Open").map { it.id })
        assertTrue(table.records("proposal", false, RecordSort.ADDED, "status", "Done").isEmpty())
        assertEquals(listOf("archive"), table.records("proposal", true, RecordSort.ADDED).map { it.id })
    }

    @Test fun customDateFieldsSortWithUndatedEntriesLast() {
        assertEquals(LocalDate.of(2026, 9, 18), table.dateOf(table.rows.first()))
        assertEquals(listOf("one", "two", "three"), table.records("", false, RecordSort.NEWEST).map { it.id })
        assertEquals(listOf("two", "one", "three"), table.records("", false, RecordSort.OLDEST).map { it.id })
    }

    @Test fun bulkArchiveAndRestoreKeepValuesAndUnselectedRows() {
        val doc = TemplateDocument(emptyList(), listOf(table))
        val archived = doc.archiveRows("work", setOf("one", "two"), true)
        assertEquals(listOf("three"), archived.table("work").rows.filterNot { it.archived }.map { it.id })
        assertEquals(table.rows.first().values, archived.table("work").rows.first().values)
        val restored = archived.archiveRows("work", setOf("one", "two"), false)
        assertEquals(doc, restored)
        assertTrue(runCatching { doc.archiveRows("work", setOf("missing"), true) }.isFailure)
    }

    @Test fun startersHaveIndependentIdsAndNoSampleRecords() {
        PageStarters.all.forEach { starter -> assertTrue(PageStarters.document(starter.id).tables.all { it.rows.isEmpty() }) }
        val first = PageStarters.document("projects")
        val second = PageStarters.document("projects")
        assertNotEquals(first.tables.single().id, second.tables.single().id)
        assertEquals("board", first.blocks.single().view)
        assertTrue(first.tables.single().fields.any { it.kind == "CHECKLIST" })
    }

    @Test fun workspaceSearchFindsNestedChecklistAndRecordValues() {
        val doc = TemplateDocument(listOf(PageBlock("group", "GROUP", "Trip", children = listOf(PageBlock("item", "CHECK", "Passport")))), listOf(table))
        assertTrue(doc.matchesQuery("passport"))
        assertTrue(doc.matchesQuery("CLIENT"))
        assertFalse(doc.matchesQuery("absent"))
    }
}

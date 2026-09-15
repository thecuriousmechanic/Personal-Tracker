package com.krishna.personalos.core.model

import com.krishna.personalos.core.database.*
import org.junit.Assert.*
import org.junit.Test

class TrackerWorkspaceTest {
    private fun field(kind: FieldKind, options: String = "") = TrackerFieldEntity("field", "tracker", "Value", kind.name, options, 0)

    @Test fun datesAndNumbersRejectInvalidValues() {
        assertNotNull(field(FieldKind.DATE).valueError("2026-02-30"))
        assertNull(field(FieldKind.DATE).valueError("2028-02-29"))
        assertNotNull(field(FieldKind.NUMBER).valueError("NaN"))
        assertNotNull(field(FieldKind.NUMBER).valueError("twelve"))
        assertNull(field(FieldKind.NUMBER).valueError("-12.50"))
        assertNull(field(FieldKind.NUMBER).valueError(""))
    }

    @Test fun choicesAndCheckboxesAreTyped() {
        val choice = field(FieldKind.CHOICE, "Started| Finished |Started")
        assertEquals(listOf("Started", "Finished"), choice.choices())
        assertNotNull(choice.valueError("Unknown"))
        assertNull(choice.valueError("Finished"))
        assertNull(field(FieldKind.CHECKBOX).valueError("false"))
        assertNotNull(field(FieldKind.CHECKBOX).valueError("yes"))
    }

    @Test fun archivedEntriesAndHiddenFieldsKeepTheirValues() {
        val snapshot = TrackerWorkspace(
            fields = listOf(field(FieldKind.TEXT).copy(archived = true)),
            entries = listOf(TrackerEntryEntity("entry", "tracker", "Keep me", createdAt = 1, archived = true)),
            cells = listOf(TrackerCellEntity("entry", "field", "Kept value")),
        )
        assertTrue(snapshot.fieldsFor("tracker").isEmpty())
        assertTrue(snapshot.entriesFor("tracker").isEmpty())
        assertEquals("Keep me", snapshot.entriesFor("tracker", archived = true).single().title)
        assertEquals("Kept value", snapshot.valuesFor("entry")["field"])
    }
}

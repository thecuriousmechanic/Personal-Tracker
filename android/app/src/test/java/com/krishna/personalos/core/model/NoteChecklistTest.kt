package com.krishna.personalos.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteChecklistTest {
    @Test fun parsesAndTogglesMarkdownChecklistWithoutChangingOtherLines() {
        val body = "Intro\n- [ ] First item\n* [x] Finished item"
        val items = body.checklistItems()

        assertEquals(listOf("First item", "Finished item"), items.map { it.label })
        assertFalse(items.first().checked)
        assertTrue(items.last().checked)
        assertEquals("Intro\n- [x] First item\n* [x] Finished item", body.toggleChecklistLine(1))
    }

    @Test fun editsChecklistLabelsAndKeepsMarkdownStorage() {
        val body = "- [ ] Buy milk"
        assertEquals("- [ ] Buy oat milk", body.renameChecklistLine(0, "Buy oat milk"))
        assertEquals("- [ ] Buy milk\n- [ ] ", body.appendNoteLine("- [ ] "))
        assertEquals("0/2 complete", body.appendNoteLine("- [ ] ").checklistSummary())
        assertEquals("", body.removeNoteLine(0))
    }
}

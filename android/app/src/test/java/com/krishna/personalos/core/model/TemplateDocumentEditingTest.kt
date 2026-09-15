package com.krishna.personalos.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateDocumentEditingTest {
    @Test fun nestedSectionsCanBeAddedMovedEditedAndDeleted() {
        val first = PageBlock("first", "TEXT", "First")
        val second = PageBlock("second", "CHECK", "Second")
        var document = TemplateDocument(listOf(PageBlock("group", "GROUP", "Group", children = listOf(first, second))), emptyList())

        document = document.addBlock(PageBlock("third", "TEXT", "Third"), "group").moveBlock("third", -1)
        assertEquals(listOf("first", "third", "second"), document.blocks.single().children.map { it.id })
        document = document.changeBlock("third") { it.copy(text = "Changed") }
        assertEquals("Changed", document.blocks.single().children[1].text)
        document = document.removeBlock("third")
        assertFalse(document.blocks.single().children.any { it.id == "third" })
        assertTrue(document.blocks.single().children.any { it.id == "second" })
    }
}

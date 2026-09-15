package com.krishna.personalos.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.krishna.personalos.core.model.*
import com.krishna.personalos.ui.theme.PersonalOSTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecordBrowserTest {
    @get:Rule val compose = createComposeRule()
    private fun document() = TemplateDocument(emptyList(), listOf(PageTable("items", "Items", listOf(PageField("status", "Status", "CHOICE", listOf("Open", "Done"))), listOf(
        PageRow("first", "First record", mapOf("status" to "Open")), PageRow("second", "Second record", mapOf("status" to "Done")),
    ))))

    @Test fun filteredSelectAllArchivesOnlyMatchingRecordsAndRestoreKeepsValues() {
        val state = mutableStateOf(document())
        compose.setContent { PersonalOSTheme { RecordBrowser(state.value, "items", false, {}, { _, _ -> }, {},
            onDelete = { ids -> state.value = state.value.removeRows("items", ids) },
            onArchive = { ids, archived -> state.value = state.value.archiveRows("items", ids, archived) },
            onDuplicate = {},
        ) } }
        compose.onNode(hasText("Open", substring = false) and !hasAnyAncestor(hasTestTag("record-list"))).performClick()
        compose.onNodeWithText("Second record").assertDoesNotExist()
        compose.onNodeWithText("Select all", substring = false).performClick()
        compose.onNodeWithText("Archive selected").performClick()
        compose.waitUntil { state.value.table("items").rows.first().archived }
        assertFalse(state.value.table("items").rows.last().archived)
        compose.onNodeWithText("Archive", substring = false).performClick()
        compose.onNodeWithText("Select all", substring = false).performClick()
        compose.onNodeWithText("Restore selected").performClick()
        compose.waitUntil { !state.value.table("items").rows.first().archived }
        assertEquals("Open", state.value.table("items").rows.first().values["status"])
    }

    @Test fun failedDeletionKeepsSelectionAndShowsRetryableError() {
        compose.setContent { PersonalOSTheme { RecordBrowser(document(), "items", false, {}, { _, _ -> }, {},
            onDelete = { error("Storage unavailable") }, onArchive = { _, _ -> }, onDuplicate = {},
        ) } }
        compose.onNodeWithText("First record").performTouchInput { longClick() }
        compose.onNodeWithTag("select-all-records").performClick()
        compose.onNodeWithTag("delete-selected-records").performClick()
        compose.onNodeWithText("Delete permanently").performClick()
        compose.onNodeWithText("Storage unavailable").assertIsDisplayed()
        compose.onNodeWithText("2 selected").assertIsDisplayed()
        compose.onNodeWithText("First record").assertExists()
    }
}

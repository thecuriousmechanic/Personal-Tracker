package com.krishna.personalos.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.importing.NotionImporter
import com.krishna.personalos.core.model.TemplateDocument
import com.krishna.personalos.core.repository.TemplateRepository
import com.krishna.personalos.ui.theme.PersonalOSTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NotionImportFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun newTemplateExposesImportAndCancelWithoutChangingData() {
        compose.setContent {
            var importing by remember { mutableStateOf(false) }
            PersonalOSTheme {
                if (importing) NotionImportDialog({ importing = false }) { _, _ -> error("No file selected") }
                else NewTemplateDialog(false, null, {}, { importing = true }) { _, _, _ -> }
            }
        }
        compose.onNodeWithTag("open-notion-import").performScrollTo().performClick()
        compose.onNodeWithText("Choose export file").assertExists()
        compose.onNodeWithTag("confirm-notion-import").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithTag("open-notion-import").assertExists()
    }
    @Test fun importedDocumentsPersistAndNeverOverwriteExistingPages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TemplateRepository(db)
            val existing = repo.create("Existing", "Keep this")
            val parsed = NotionImporter.read("Tasks.csv", "Name,Status\nShip,Done".byteInputStream())
            val first = repo.importDocument("Imported", parsed.selected(true))
            val second = repo.importDocument("Imported", parsed.selected(false))
            assertNotEquals(first, second)
            assertEquals("Existing", db.templatePageDao().page(existing)!!.title)
            assertEquals("Ship", TemplateDocument.parse(db.templatePageDao().page(first)!!.data).tables.single().rows.single().title)
            assertTrue(TemplateDocument.parse(db.templatePageDao().page(second)!!.data).tables.single().rows.isEmpty())
        } finally { db.close() }
    }
}

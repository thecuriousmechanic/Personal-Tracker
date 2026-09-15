package com.krishna.personalos.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.TemplateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.YearMonth

class TemplateRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var repository: TemplateRepository
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TemplateRepository(database)
    }
    @After fun close() = database.close()

    @Test fun privateImportIsCompleteAndDoesNotOverwriteEdits() = runBlocking {
        Assume.assumeTrue("Private import is intentionally excluded from source control", "templates.json" in context.assets.list("private-notion").orEmpty())
        repository.importBundled(context)
        val pages = repository.pages.first()
        assertEquals(5, pages.size)
        assertEquals(0, pages.sumOf { TemplateDocument.parse(it.data).tables.sumOf { t -> t.rows.size } })
        assertEquals(24, TemplateDocument.parse(pages.first { it.id == "obsidian-24-week-plan" }.data).blocks.count { it.kind == "GROUP" })
        assertEquals(724, TemplateDocument.parse(pages.first { it.id == "obsidian-24-week-plan" }.data).blocks.sumOf { it.children.size })
        assertEquals(55, TemplateDocument.parse(pages.first { it.id == "obsidian-syllabus" }.data).blocks.sumOf { it.children.size })
        val page = pages.first()
        repository.rename(page.id, "My edited template", page.subtitle)
        repository.change(page.id) { it.copy(blocks = it.blocks + PageBlock("local", "CHECK", "Local edit", checked = true)) }
        val databasePage = pages.first { TemplateDocument.parse(it.data).tables.isNotEmpty() }
        val databaseId = TemplateDocument.parse(databasePage.data).tables.first().id
        repository.change(databasePage.id) { it.changeTable(databaseId) { table -> table.copy(rows = table.rows + PageRow("my-record", "My record", emptyMap())) } }
        repository.importBundled(context)
        assertEquals(5, repository.pages.first().size)
        assertEquals("My edited template", database.templatePageDao().page(page.id)!!.title)
        assertTrue(TemplateDocument.parse(database.templatePageDao().page(page.id)!!.data).blocks.last().checked)
        assertEquals(listOf("my-record"), TemplateDocument.parse(database.templatePageDao().page(databasePage.id)!!.data).table(databaseId).rows.map { it.id })
    }

    @Test fun editsUpdateRelationsAndTotalsAndCopiesRemainIndependent() = runBlocking {
        seed()
        val copy = repository.duplicate("source")
        repository.saveRow(copy, "expenses", PageRow("expense", "Updated", mapOf("Amount" to "18.25", "Date" to "2026-09-01", "Budget" to "[\"category\"]")))
        val doc = document(copy)
        assertEquals("18.25", doc.budgetSpent(doc.table("budget").rows.single(), YearMonth.of(2026, 9)).toPlainString())
        assertEquals(listOf("expense"), doc.references(doc.table("budget").rows.single(), "Expenses"))
        assertEquals("5", document("source").table("expenses").rows.single().values["Amount"])
        val category = doc.table("budget").rows.single().copy(values = mapOf("Monthly Budget" to "100", "Expenses" to "[]"))
        repository.saveRow(copy, "budget", category)
        assertTrue(document(copy).references(document(copy).table("expenses").rows.single(), "Budget").isEmpty())
        assertEquals("0", document(copy).budgetSpent(category, YearMonth.of(2026, 9)).toPlainString())
    }

    @Test fun invalidRecordLeavesSavedValuesUntouched() = runBlocking {
        seed()
        val row = document("source").table("expenses").rows.single()
        assertTrue(runCatching { repository.saveRow("source", "expenses", row.copy(values = row.values + ("Amount" to "oops"))) }.isFailure)
        assertTrue(runCatching { repository.saveRow("source", "expenses", row.copy(values = row.values + ("Date" to "2026-09-01junk"))) }.isFailure)
        assertEquals(row, document("source").table("expenses").rows.single())
    }

    @Test fun entryCanBePermanentlyDeletedWithoutLeavingRelations() = runBlocking {
        seed()
        repository.saveRow("source", "expenses", document("source").table("expenses").rows.single().copy(values = mapOf("Amount" to "5", "Date" to "2026-09-01", "Budget" to "[\"category\"]")))

        repository.deleteRow("source", "expenses", "expense")

        val changed = document("source")
        assertTrue(changed.table("expenses").rows.isEmpty())
        assertTrue(changed.references(changed.table("budget").rows.single(), "Expenses").isEmpty())
    }

    @Test fun multipleEntriesCanBeDeletedTogetherAndRelationsAreCleaned() = runBlocking {
        seed()
        repository.saveRow("source", "expenses", PageRow("second", "Second", mapOf("Amount" to "8", "Date" to "2026-09-02", "Budget" to "[\"category\"]")))
        repository.deleteRows("source", "expenses", setOf("expense", "second"))
        val changed = document("source")
        assertTrue(changed.table("expenses").rows.isEmpty())
        assertTrue(changed.references(changed.table("budget").rows.single(), "Expenses").isEmpty())
    }

    @Test fun blankTemplateCanBeCreatedLocally() = runBlocking {
        val id = repository.create("My page", "Local workspace")
        val saved = database.templatePageDao().page(id)!!
        assertEquals("My page", saved.title)
        assertEquals("Local workspace", saved.subtitle)
        assertTrue(TemplateDocument.parse(saved.data).blocks.isEmpty())
        assertTrue(TemplateDocument.parse(saved.data).tables.isEmpty())
    }

    @Test fun pageEditsSurviveDatabaseReopen() = runBlocking {
        val name = "native-template-persistence"
        context.deleteDatabase(name)
        val disk = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            disk.templatePageDao().importPage(entity())
            TemplateRepository(disk).change("source") { it.copy(blocks = listOf(PageBlock("check", "CHECK", "Saved", checked = true))) }
        } finally { disk.close() }
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try { assertTrue(TemplateDocument.parse(reopened.templatePageDao().page("source")!!.data).blocks.single().checked) }
        finally { reopened.close(); context.deleteDatabase(name) }
    }

    private suspend fun seed() = database.templatePageDao().importPage(entity())
    private suspend fun document(id: String) = TemplateDocument.parse(database.templatePageDao().page(id)!!.data)
    private fun entity(): TemplatePageEntity {
        val doc = TemplateDocument(emptyList(), listOf(
            PageTable("budget", "Categories", listOf(PageField("Expenses", "Expenses", "RELATION", relation = "expenses"), PageField("Monthly Budget", "Budget", "NUMBER")), listOf(PageRow("category", "Books", mapOf("Monthly Budget" to "100", "Expenses" to "[]")))),
            PageTable("expenses", "Expenses", listOf(PageField("Amount", "Amount", "NUMBER"), PageField("Date", "Date", "DATE"), PageField("Budget", "Category", "RELATION", relation = "budget")), listOf(PageRow("expense", "Book", mapOf("Amount" to "5", "Date" to "2026-09-01", "Budget" to "[]"))))
        ))
        return TemplatePageEntity("source", "Finance", "", "finance", "", "", "2026-09-09", doc.toJson())
    }
}

package com.krishna.personalos.core.repository

import android.content.Context
import androidx.room.withTransaction
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import java.time.LocalDate
import java.util.UUID

class TemplateRepository(private val database: AppDatabase) {
    private val dao = database.templatePageDao()
    val pages = dao.observe().map { pages ->
        pages.forEach { TemplateDocumentCache.getOrParse(it.id, it.data) }
        pages
    }.flowOn(Dispatchers.Default)

    suspend fun delete(id: String) = database.withTransaction {
        dao.page(id)?.takeIf { !it.deleted }?.let {
            RecoveryRepository(database).capture("page", it.title, id, mapOf("template_pages" to ("id = ?" to arrayOf(id))))
            dao.update(it.copy(deleted = true, data = "{\"blocks\":[],\"tables\":[]}"))
        }
        TemplateDocumentCache.remove(id)
    }
    /** Explicit, non-destructive re-import of the verified Notion finance template. */
    suspend fun importFinance(context: Context): String = withContext(Dispatchers.IO) {
        val source = context.assets.open("private-notion/templates.json").bufferedReader().use { JSONArray(it.readText()).objects() }
            .single { it.getString("kind") == "finance" }
        val id = source.getString("id")
        val original = TemplateDocument.parse(source.getJSONObject("data").toString())
        // Categories are structure, not transactions. Never copy sample amounts or links.
        val clean = original.copy(tables = original.tables.map { table -> table.copy(rows =
            if (table.id == "budget") table.rows.map { PageRow("finance-category-${it.id}", it.title, emptyMap()) }
            else emptyList()) })
        database.withTransaction {
            val existing = dao.page(id)
            if (existing == null || existing.deleted) {
                val page = TemplatePageEntity(id, source.getString("title"), "Budgets, expenses, income and monthly summaries", "finance", source.optString("cover"), source.getString("sourceUrl"), LocalDate.now().toString(), clean.toJson())
                if (existing == null) dao.importPage(page) else dao.update(page)
                TemplateDocumentCache.put(id, page.data, clean)
            } else {
                val current = TemplateDocument.parse(existing.data)
                val tables = current.tables.map { table ->
                    if (table.id == "budget" && table.rows.isEmpty()) table.copy(rows = clean.table("budget").rows) else table
                } + clean.tables.filter { candidate -> current.tables.none { it.id == candidate.id } }
                val updated = current.copy(tables = tables)
                dao.update(existing.copy(archived = false, data = updated.toJson()))
                TemplateDocumentCache.put(id, updated.toJson(), updated)
            }
        }
        id
    }
    suspend fun importBundled(context: Context) = withContext(Dispatchers.IO) {
        val files = context.assets.list("private-notion").orEmpty()
        if ("templates.json" !in files) return@withContext
        val imported = context.assets.open("private-notion/templates.json").bufferedReader().use { JSONArray(it.readText()).objects() }
        database.withTransaction {
            imported.forEach { p ->
                val source = TemplateDocument.parse(p.getJSONObject("data").toString())
                val sampleRowIds = source.tables.flatMap { table -> table.rows.map { it.id } }.toSet()
                val emptySource = source.copy(tables = source.tables.map { it.copy(rows = emptyList()) })
                val page = TemplatePageEntity(p.getString("id"), p.getString("title"), p.getString("subtitle"), p.getString("kind"), p.optString("cover"), p.getString("sourceUrl"), p.getString("importedAt"), emptySource.toJson())
                val existing = dao.page(page.id)
                if (existing == null) {
                    dao.importPage(page)
                    TemplateDocumentCache.put(page.id, page.data, emptySource)
                } else if (!existing.deleted) {
                    val current = TemplateDocumentCache.getOrParse(existing.id, existing.data)
                    val cleaned = current.withoutRows(sampleRowIds)
                    if (cleaned != current) {
                        val json = cleaned.toJson()
                        dao.update(existing.copy(data = json))
                        TemplateDocumentCache.put(existing.id, json, cleaned)
                    }
                }
            }
            importObsidianPage(context, "obsidian-syllabus", "Syllabus", "Mathematics foundations, calculus, linear algebra and probability", "obsidian/syllabus.md", false)
            importObsidianPage(context, "obsidian-24-week-plan", "24 Week Plan", "A complete 24-week technical learning roadmap", "obsidian/24-week-plan.md", true)
        }
    }

    private suspend fun importObsidianPage(context: Context, id: String, title: String, subtitle: String, asset: String, weekly: Boolean) {
        if (asset.substringAfterLast('/') !in context.assets.list("obsidian").orEmpty()) return
        val source = context.assets.open(asset).bufferedReader().use { it.readText() }
        val document = obsidianDocument(id, source, weekly)
        dao.importPage(TemplatePageEntity(id, title, subtitle, "study", "", "Obsidian · Vector Hub", "2026-09-15", document.toJson()))
        TemplateDocumentCache.put(id, document.toJson(), document)
    }

    private fun obsidianDocument(pageId: String, source: String, weekly: Boolean): TemplateDocument {
        val roots = mutableListOf<PageBlock>()
        var groupTitle: String? = null
        var children = mutableListOf<PageBlock>()
        var sequence = 0
        fun nextId() = "$pageId-${sequence++}"
        fun flushGroup() {
            groupTitle?.let { roots += PageBlock(nextId(), "GROUP", it, children = children.toList()) }
            groupTitle = null
            children = mutableListOf()
        }
        source.lineSequence().map(String::trim).filter(String::isNotBlank).forEach { line ->
            val check = Regex("^\\[([^]]*)]\\s*(.*)$").matchEntire(line)
            if (check != null) {
                if (groupTitle == null) groupTitle = if (weekly) "Plan" else "Topics"
                val marker = check.groupValues[1]
                children += PageBlock(nextId(), "CHECK", check.groupValues[2], checked = marker.contains('✓') || marker.contains('x', true))
            } else {
                flushGroup()
                if (!weekly && line.matches(Regex("^\\d+\\..*"))) roots += PageBlock(nextId(), "HEADING", line)
                else groupTitle = line
            }
        }
        flushGroup()
        return TemplateDocument(roots, emptyList())
    }
    suspend fun change(id: String, transform: (TemplateDocument) -> TemplateDocument) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val page = requireNotNull(dao.page(id))
            require(!page.deleted) { "This template was deleted" }
            val before = TemplateDocumentCache.getOrParse(page.id, page.data)
            val after = transform(before)
            after.tables.forEach { table ->
                require(table.fields.map { it.id }.distinct().size == table.fields.size) { "Each field needs a unique identifier" }
                require(table.fields.all { it.label.isNotBlank() }) { "Give every field a name" }
                table.fields.filter { it.kind == "CHOICE" }.forEach { field ->
                    val old = before.tables.find { it.id == table.id }?.fields?.find { it.id == field.id }
                    if (old != null && old.options != field.options) {
                        require(table.rows.none { it.values[field.id].orEmpty().let { value -> value.isNotBlank() && value !in field.options } }) { "Keep choices that are used by existing records" }
                    }
                }
            }
            val json = after.toJson()
            RecoveryRepository(database).captureRemovedSections(page, before, after)
            dao.update(page.copy(data = json))
            TemplateDocumentCache.put(page.id, json, after)
        }
    }
    suspend fun rename(id: String, title: String, subtitle: String) {
        require(title.isNotBlank())
        database.withTransaction { dao.page(id)?.let { dao.update(it.copy(title = title.trim(), subtitle = subtitle.trim())) } }
    }
    suspend fun create(title: String, subtitle: String, starter: String = "blank"): String = database.withTransaction {
        require(title.isNotBlank()) { "Give your template a name" }
        val id = UUID.randomUUID().toString()
        val document = PageStarters.document(starter)
        val json = document.toJson()
        val kind = when (starter) { "finance" -> "finance"; "routine" -> "week"; "reading" -> "study"; else -> "personal" }
        dao.importPage(TemplatePageEntity(id, title.trim(), subtitle.trim(), kind, "", "Local page", LocalDate.now().toString(), json))
        TemplateDocumentCache.put(id, json, document)
        id
    }
    suspend fun importDocument(title: String, document: TemplateDocument): String = withContext(Dispatchers.IO) {
        require(title.isNotBlank()) { "Give your template a name" }
        val id = UUID.randomUUID().toString()
        val json = document.toJson()
        database.withTransaction {
            dao.importPage(TemplatePageEntity(id, title.trim(), "Imported from a Notion export · Offline copy", "personal", "", "Notion Markdown & CSV export", LocalDate.now().toString(), json))
            TemplateDocumentCache.put(id, json, document)
        }
        id
    }
    suspend fun archive(id: String, archived: Boolean) = database.withTransaction { dao.page(id)?.let { dao.update(it.copy(archived = archived)) } }
    suspend fun duplicate(id: String): String = database.withTransaction {
        val page = requireNotNull(dao.page(id))
        val copy = UUID.randomUUID().toString()
        dao.importPage(page.copy(id = copy, title = "${page.title} copy", archived = false))
        copy
    }
    suspend fun saveRow(pageId: String, tableId: String, row: PageRow) {
        change(pageId) { doc ->
            val table = doc.table(tableId)
            require(row.title.isNotBlank() || table.dateOf(row) != null) { "Add a name or date" }
            table.fields.filter { !it.hidden }.forEach { f ->
                val v = row.values[f.id].orEmpty()
                if (v.isNotBlank()) when (f.kind) {
                    "NUMBER" -> require(v.toBigDecimalOrNull() != null) { "${f.label}: enter a number" }
                    "DATE" -> require(runCatching { java.time.LocalDate.parse(v) }.isSuccess || runCatching { java.time.OffsetDateTime.parse(v) }.isSuccess) { "${f.label}: use YYYY-MM-DD" }
                    "CHECKBOX" -> require(v in listOf("true", "false"))
                    "CHOICE" -> require(v in f.options) { "Choose an available option" }
                }
            }
            var updated = doc.changeTable(tableId) { it.copy(rows = if (it.rows.any { old -> old.id == row.id }) it.rows.map { old -> if (old.id == row.id) row else old } else it.rows + row) }
            // Imported two-way relations must agree whichever end the user edits.
            table.fields.filter { it.kind == "RELATION" }.forEach { field ->
                val target = updated.tables.find { it.id == field.relation }
                val inverse = target?.fields?.singleOrNull { it.kind == "RELATION" && it.relation == tableId }
                if (target != null && inverse != null) {
                    val links = updated.references(row, field.id)
                    updated = updated.changeTable(target.id) { t -> t.copy(rows = t.rows.map { related ->
                        val existing = updated.references(related, inverse.id)
                        val next = if (related.id in links) (existing + row.id).distinct() else existing - row.id
                        related.copy(values = related.values + (inverse.id to JSONArray(next).toString()))
                    }) }
                }
            }
            updated
        }
    }


    suspend fun deleteRow(pageId: String, tableId: String, rowId: String) {
        deleteRows(pageId, tableId, setOf(rowId))
    }

    suspend fun deleteRows(pageId: String, tableId: String, rowIds: Set<String>) {
        database.withTransaction {
            val page = requireNotNull(dao.page(pageId))
            require(!page.deleted) { "Restore the page first" }
            val doc = TemplateDocumentCache.getOrParse(page.id, page.data)
            val rows = doc.table(tableId).rows.filter { it.id in rowIds }
            require(rows.size == rowIds.size && rows.isNotEmpty()) { "Choose existing records" }
            RecoveryRepository(database).captureRows(page, tableId, rows)
            change(pageId) { document -> document.removeRows(tableId, rowIds) }
        }
    }
}

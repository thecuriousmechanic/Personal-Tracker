package com.krishna.personalos.core.model

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class PageBlock(val id: String, val kind: String, val text: String, val table: String = "", val view: String = "", val checked: Boolean = false, val hidden: Boolean = false, val children: List<PageBlock> = emptyList())
data class PageField(val id: String, val label: String, val kind: String, val options: List<String> = emptyList(), val relation: String = "", val hidden: Boolean = false)
data class PageRow(val id: String, val title: String, val values: Map<String, String>, val archived: Boolean = false) {
    fun displayTitle() = title.ifBlank { values["Date"].orEmpty().ifBlank { "Untitled" } }
    private val parsedDate by lazy { values["Date"]?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } }
    fun date() = parsedDate
}
data class PageTable(val id: String, val title: String, val fields: List<PageField>, val rows: List<PageRow>) {
    val activeByDate by lazy { rows.filter { !it.archived && it.date() != null }.groupBy { it.date() } }
}
data class TemplateDocument(val blocks: List<PageBlock>, val tables: List<PageTable>) {
    fun table(id: String) = tables.first { it.id == id }
    fun changeTable(id: String, change: (PageTable) -> PageTable) = copy(tables = tables.map { if (it.id == id) change(it) else it })
    fun changeBlock(id: String, change: (PageBlock) -> PageBlock): TemplateDocument {
        fun edit(blocks: List<PageBlock>): List<PageBlock> = blocks.map { if (it.id == id) change(it) else it.copy(children = edit(it.children)) }
        return copy(blocks = edit(blocks))
    }
    fun removeBlock(id: String): TemplateDocument {
        fun remove(items: List<PageBlock>): List<PageBlock> = items.filterNot { it.id == id }.map { it.copy(children = remove(it.children)) }
        return copy(blocks = remove(blocks))
    }
    fun addBlock(block: PageBlock, parentId: String? = null): TemplateDocument =
        if (parentId == null) copy(blocks = blocks + block)
        else changeBlock(parentId) { parent -> parent.copy(children = parent.children + block) }

    fun removeRow(tableId: String, rowId: String): TemplateDocument = removeRows(tableId, setOf(rowId))

    fun removeRows(tableId: String, rowIds: Set<String>): TemplateDocument {
        require(rowIds.isNotEmpty()) { "Select at least one entry" }
        val existing = tables.find { it.id == tableId }?.rows.orEmpty().map { it.id }.toSet()
        require(existing.containsAll(rowIds)) { "One or more entries no longer exist" }
        return withoutRows(rowIds)
    }

    /** Used when removing rows that were bundled as sample data across several tables. */
    fun withoutRows(rowIds: Set<String>): TemplateDocument {
        if (rowIds.isEmpty()) return this
        return copy(tables = tables.map { table ->
            val relationFields = table.fields.filter { it.kind == "RELATION" }
            val remainingRows = table.rows.filterNot { it.id in rowIds }
            table.copy(rows = remainingRows.map { row ->
                if (relationFields.isEmpty()) row else row.copy(values = relationFields.fold(row.values) { values, field ->
                    val links = runCatching { JSONArray(values[field.id] ?: "[]").strings() }.getOrDefault(emptyList())
                    if (links.none { it in rowIds }) values else values + (field.id to JSONArray(links.filterNot { it in rowIds }).toString())
                })
            })
        })
    }

    fun moveBlock(id: String, offset: Int): TemplateDocument {
        fun move(items: List<PageBlock>): Pair<List<PageBlock>, Boolean> {
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) {
                val destination = (index + offset).coerceIn(items.indices)
                if (destination == index) return items to true
                val changed = items.toMutableList()
                val item = changed.removeAt(index)
                changed.add(destination, item)
                return changed to true
            }
            var found = false
            val changed = items.map { item ->
                if (found) item else {
                    val (children, childFound) = move(item.children)
                    if (childFound) found = true
                    item.copy(children = children)
                }
            }
            return changed to found
        }
        return copy(blocks = move(blocks).first)
    }
    fun completion(row: PageRow, table: PageTable): Float {
        val fields = table.fields.filter { it.kind == "CHECKBOX" && !it.hidden }
        return if (fields.isEmpty()) 0f else fields.count { row.values[it.id] == "true" }.toFloat() / fields.size
    }
    fun references(row: PageRow, field: String): List<String> = runCatching { JSONArray(row.values[field] ?: "[]").strings() }.getOrDefault(emptyList())
    fun amount(row: PageRow, field: String = "Amount") = row.values[field]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    fun total(tableId: String, month: YearMonth): BigDecimal = tables.find { it.id == tableId }?.rows.orEmpty().filter { !it.archived && it.date()?.let(YearMonth::from) == month }.fold(BigDecimal.ZERO) { sum, row -> sum + amount(row) }
    fun budgetSpent(category: PageRow, month: YearMonth) = table("expenses").rows.filter { !it.archived && category.id in references(it, "Budget") && it.date()?.let(YearMonth::from) == month }.fold(BigDecimal.ZERO) { sum, row -> sum + amount(row) }
    fun computed(table: PageTable, row: PageRow, field: PageField, month: YearMonth): String = when {
        table.id == "daily" -> "${(completion(row, table) * 100).toInt()}%"
        table.id == "budget" && field.id == "This Month" -> budgetSpent(row, month).toPlainString()
        table.id == "budget" && field.id == "Utilization" -> {
            val limit = amount(row, "Monthly Budget")
            if (limit.signum() <= 0) "No budget set" else "${budgetSpent(row, month).multiply(BigDecimal(100)).divide(limit, 0, java.math.RoundingMode.HALF_UP)}%"
        }
        table.id == "habitmonths" -> {
            val linked = references(row, "Habit Tracker ").toHashSet()
            val daily = table("daily")
            val records = daily.rows.filter { !it.archived && it.id in linked }
            if (records.isEmpty()) "0%" else "${(records.map { completion(it, daily) }.average() * 100).toInt()}%"
        }
        table.id == "months" -> {
            val expenses = table("expenses").rows.filter { !it.archived && row.id in references(it, "Month Classification") }.fold(BigDecimal.ZERO) { sum, item -> sum + amount(item) }
            val incomes = table("incomes").rows.filter { !it.archived && row.id in references(it, "Month Classification") }.fold(BigDecimal.ZERO) { sum, item -> sum + amount(item) }
            when { field.id.contains("Expense") -> expenses.toPlainString(); field.id.contains("Income") -> incomes.toPlainString(); field.id.contains("Net") -> (incomes - expenses).toPlainString(); else -> "—" }
        }
        table.id == "expenses" && field.id == "This Month" -> if (row.date()?.let(YearMonth::from) == month) amount(row).toPlainString() else "0"
        else -> "—"
    }
    fun displayValue(table: PageTable, row: PageRow, field: PageField, month: YearMonth): String = when (field.kind) {
        "FORMULA" -> computed(table, row, field, month)
        "RELATION" -> { val ids = references(row, field.id); tables.find { it.id == field.relation }?.rows.orEmpty().filter { it.id in ids }.joinToString { it.displayTitle() }.ifBlank { if (ids.isEmpty()) "—" else "${ids.size} linked records" } }
        "CHECKBOX" -> if (row.values[field.id] == "true") "✓" else "—"
        "CHECKLIST" -> row.values[field.id].orEmpty().checklistSummary()
        else -> row.values[field.id].orEmpty().ifBlank { "—" }
    }
    fun toJson(): String = JSONObject().put("blocks", JSONArray(blocks.map(::blockJson))).put("tables", JSONArray(tables.map { t ->
        JSONObject().put("id", t.id).put("title", t.title).put("fields", JSONArray(t.fields.map { f -> JSONObject().put("id", f.id).put("label", f.label).put("kind", f.kind).put("relation", f.relation).put("hidden", f.hidden).put("options", JSONArray(f.options)) }))
            .put("rows", JSONArray(t.rows.map { r -> JSONObject().put("id", r.id).put("title", r.title).put("archived", r.archived).put("values", JSONObject(r.values)) }))
    })).toString()
    companion object {
        fun parse(json: String): TemplateDocument {
            val root = JSONObject(json)
            return TemplateDocument(root.getJSONArray("blocks").objects().map(::parseBlock), root.getJSONArray("tables").objects().map { t ->
                PageTable(t.getString("id"), t.getString("title"), t.getJSONArray("fields").objects().map { f -> PageField(f.getString("id"), f.getString("label"), f.getString("kind"), f.optJSONArray("options")?.strings().orEmpty(), f.optString("relation"), f.optBoolean("hidden")) },
                    t.getJSONArray("rows").objects().map { r -> PageRow(r.getString("id"), r.optString("title"), r.getJSONObject("values").let { v -> v.keys().asSequence().associateWith { v.optString(it) } }, r.optBoolean("archived")) })
            })
        }
    }
}

private fun parseBlock(b: JSONObject): PageBlock = PageBlock(b.getString("id"), b.getString("kind"), b.optString("text"), b.optString("table"), b.optString("view"), b.optBoolean("checked"), b.optBoolean("hidden"), b.optJSONArray("children")?.objects()?.map(::parseBlock).orEmpty())
private fun blockJson(b: PageBlock): JSONObject = JSONObject().put("id", b.id).put("kind", b.kind).put("text", b.text).put("table", b.table).put("view", b.view).put("checked", b.checked).put("hidden", b.hidden).put("children", JSONArray(b.children.map(::blockJson)))
fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)

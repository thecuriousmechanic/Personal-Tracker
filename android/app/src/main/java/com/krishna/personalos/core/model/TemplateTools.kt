package com.krishna.personalos.core.model

import java.time.LocalDate
import java.util.UUID

/** Dates belong to the database schema, including user-created date fields. */
fun PageTable.dateOf(row: PageRow): LocalDate? {
    val field = fields.firstOrNull { it.kind == "DATE" && !it.hidden } ?: return null
    return runCatching { LocalDate.parse(row.values[field.id].orEmpty().take(10)) }.getOrNull()
}

fun PageTable.records(query: String, archived: Boolean, sort: RecordSort, choiceField: String? = null, choice: String? = null): List<PageRow> {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val matching = rows.filter { row ->
        row.archived == archived && (choiceField == null || choice == null || row.values[choiceField].orEmpty() == choice) &&
            terms.all { term -> row.displayTitle().contains(term, true) || row.values.values.any { it.contains(term, true) } }
    }
    return when (sort) {
        RecordSort.TITLE -> matching.sortedBy { it.displayTitle().lowercase() }
        RecordSort.NEWEST -> matching.sortedWith(compareBy<PageRow> { dateOf(it) == null }.thenByDescending { dateOf(it) }.thenBy { it.displayTitle().lowercase() })
        RecordSort.OLDEST -> matching.sortedWith(compareBy<PageRow> { dateOf(it) == null }.thenBy { dateOf(it) }.thenBy { it.displayTitle().lowercase() })
        RecordSort.ADDED -> matching
    }
}

enum class RecordSort(val label: String) { ADDED("Added order"), TITLE("Name A–Z"), NEWEST("Newest date"), OLDEST("Oldest date") }

fun TemplateDocument.matchesQuery(query: String): Boolean {
    fun blocksMatch(blocks: List<PageBlock>): Boolean = blocks.any { it.text.contains(query, true) || blocksMatch(it.children) }
    return blocksMatch(blocks) || tables.any { table -> table.title.contains(query, true) || table.rows.any { row -> row.title.contains(query, true) || row.values.values.any { it.contains(query, true) } } }
}

fun TemplateDocument.archiveRows(tableId: String, ids: Set<String>, archived: Boolean): TemplateDocument {
    require(ids.isNotEmpty() && table(tableId).rows.map { it.id }.containsAll(ids)) { "Selected records no longer exist" }
    return changeTable(tableId) { table -> table.copy(rows = table.rows.map { if (it.id in ids) it.copy(archived = archived) else it }) }
}

data class PageStarter(val id: String, val title: String, val description: String)

object PageStarters {
    val all = listOf(
        PageStarter("blank", "Blank page", "Build your own sections and databases."),
        PageStarter("routine", "Daily routine", "Check off daily habits and see your progress."),
        PageStarter("finance", "Budget & spending", "Budgets, expenses, income, and monthly totals."),
        PageStarter("projects", "Project board", "Organize work by status with dates and checklists."),
        PageStarter("reading", "Reading list", "Keep sources, progress, and reading notes together."),
        PageStarter("review", "Weekly review", "Reflect on your week and plan the next one."),
    )

    fun document(id: String): TemplateDocument {
        require(all.any { it.id == id }) { "Choose a template starter" }
        fun block(kind: String, text: String) = PageBlock(UUID.randomUUID().toString(), kind, text)
        if (id == "blank") return TemplateDocument(emptyList(), emptyList())
        if (id == "routine") {
            val fields = listOf(PageField("Date", "Date", "DATE"), PageField("Read", "Read", "CHECKBOX"), PageField("Move", "Move", "CHECKBOX"), PageField("Reflect", "Reflect", "CHECKBOX"), PageField("Notes", "Notes", "TEXT"), PageField("Progress", "Progress", "FORMULA"))
            return TemplateDocument(listOf(PageBlock(UUID.randomUUID().toString(), "DAILY", "Daily check-in", "daily"), PageBlock(UUID.randomUUID().toString(), "CALENDAR", "Your month", "daily")), listOf(PageTable("daily", "Daily routine", fields, emptyList())))
        }
        if (id == "finance") {
            val budget = PageTable("budget", "Budgets", listOf(PageField("Monthly Budget", "Monthly budget", "NUMBER"), PageField("This Month", "Spent this month", "FORMULA"), PageField("Utilization", "Budget used", "FORMULA"), PageField("expenses", "Expenses", "RELATION", relation = "expenses")), emptyList())
            val expenses = PageTable("expenses", "Expenses", listOf(PageField("Date", "Date", "DATE"), PageField("Amount", "Amount", "NUMBER"), PageField("Budget", "Budget category", "RELATION", relation = "budget"), PageField("Notes", "Notes", "TEXT")), emptyList())
            val incomes = PageTable("incomes", "Income", listOf(PageField("Date", "Date", "DATE"), PageField("Amount", "Amount", "NUMBER"), PageField("Type", "Type", "CHOICE", listOf("Salary", "Freelance", "Other")), PageField("Notes", "Notes", "TEXT")), emptyList())
            return TemplateDocument(listOf(block("SUMMARY", "This month"), block("QUICK_ADD", "Log a transaction"), PageBlock(UUID.randomUUID().toString(), "TABLE", "Budgets", "budget", "budget"), PageBlock(UUID.randomUUID().toString(), "TABLE", "Expenses", "expenses", "list"), PageBlock(UUID.randomUUID().toString(), "TABLE", "Income", "incomes", "list")), listOf(budget, expenses, incomes))
        }
        if (id == "review") return TemplateDocument(listOf(
            block("HEADING", "This week's focus"), block("TEXT", ""),
            block("HEADING", "Reflection"), block("TEXT", ""),
            block("HEADING", "Next week"), block("CHECK", "Set next week's priorities"),
        ), emptyList())
        val tableId = UUID.randomUUID().toString()
        val title = if (id == "projects") "Projects" else "Reading"
        val fields = if (id == "projects") listOf(
            PageField("status", "Status", "CHOICE", listOf("Not started", "In progress", "Done")),
            PageField("Date", "Due date", "DATE"),
            PageField("priority", "Priority", "CHOICE", listOf("High", "Medium", "Low")),
            PageField("checklist", "Steps", "CHECKLIST"), PageField("notes", "Notes", "TEXT"),
        ) else listOf(
            PageField("status", "Status", "CHOICE", listOf("To read", "Reading", "Finished")),
            PageField("author", "Author or source", "TEXT"), PageField("progress", "Progress %", "NUMBER"),
            PageField("Date", "Started", "DATE"), PageField("notes", "Notes", "TEXT"),
        )
        return TemplateDocument(listOf(PageBlock(UUID.randomUUID().toString(), "TABLE", title, tableId, if (id == "projects") "board" else "list")), listOf(PageTable(tableId, title, fields, emptyList())))
    }
}

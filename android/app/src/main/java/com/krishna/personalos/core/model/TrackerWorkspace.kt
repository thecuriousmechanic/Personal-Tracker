package com.krishna.personalos.core.model

import com.krishna.personalos.core.database.*
import java.time.LocalDate

enum class FieldKind(val label: String) { TEXT("Text"), NUMBER("Number"), CHECKBOX("Checkbox"), CHECKLIST("Checklist"), DATE("Date"), CHOICE("Choice") }

fun TrackerFieldEntity.choices(): List<String> = options.split("|").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

fun TrackerFieldEntity.valueError(value: String): String? {
    if (value.isBlank()) return null
    return when (FieldKind.valueOf(kind)) {
        FieldKind.TEXT -> null
        FieldKind.NUMBER -> if (value.toBigDecimalOrNull() == null) "Enter a number" else null
        FieldKind.DATE -> if (runCatching { LocalDate.parse(value) }.isFailure) "Use YYYY-MM-DD" else null
        FieldKind.CHECKBOX -> if (value !in listOf("true", "false")) "Choose checked or unchecked" else null
        FieldKind.CHECKLIST -> null
        FieldKind.CHOICE -> if (value !in choices()) "Choose an available option" else null
    }
}

data class TrackerWorkspace(
    val trackers: List<TrackerEntity> = emptyList(),
    val fields: List<TrackerFieldEntity> = emptyList(),
    val entries: List<TrackerEntryEntity> = emptyList(),
    val cells: List<TrackerCellEntity> = emptyList(),
) {
    fun fieldsFor(id: String) = fields.filter { it.trackerId == id && !it.archived }.sortedBy { it.position }
    fun entriesFor(id: String, archived: Boolean = false) = entries.filter { it.trackerId == id && it.archived == archived }
    fun valuesFor(id: String) = cells.filter { it.entryId == id }.associate { it.fieldId to it.value }
}

data class StarterField(val name: String, val kind: FieldKind, val options: String = "")
data class TrackerStarter(val id: String, val title: String, val description: String, val fields: List<StarterField>)

object TrackerStarters {
    val all = listOf(
        TrackerStarter("weekly", "Weekly routine", "Plan a week. Check off each day and keep a weekly reflection.",
            listOf(StarterField("Week of", FieldKind.DATE)) + listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map { StarterField(it, FieldKind.CHECKBOX) }),
        TrackerStarter("habits", "Daily habits", "One entry per day, with the habits that matter to you.", listOf(
            StarterField("Date", FieldKind.DATE), StarterField("Read", FieldKind.CHECKBOX), StarterField("Move", FieldKind.CHECKBOX),
            StarterField("Reflect", FieldKind.CHECKBOX), StarterField("Sleep hours", FieldKind.NUMBER))),
        TrackerStarter("expenses", "Spending log", "Record income and expenses. Make the categories your own.", listOf(
            StarterField("Date", FieldKind.DATE), StarterField("Amount", FieldKind.NUMBER),
            StarterField("Category", FieldKind.CHOICE, "Food|Travel|Home|Learning|Other"), StarterField("Type", FieldKind.CHOICE, "Expense|Income"))),
        TrackerStarter("reading", "Reading & learning", "Keep books, courses, and ideas together with your own notes.", listOf(
            StarterField("Status", FieldKind.CHOICE, "Want to start|In progress|Finished"), StarterField("Author or source", FieldKind.TEXT),
            StarterField("Progress %", FieldKind.NUMBER), StarterField("Started", FieldKind.DATE))),
        TrackerStarter("blank", "Start from scratch", "A blank tracker. Add fields for anything you want to follow.", emptyList()),
    )
}

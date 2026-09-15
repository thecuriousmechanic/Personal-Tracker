package com.krishna.personalos.core.model

enum class BlockPlacement { BEFORE, AFTER, INSIDE }

fun TemplateDocument.relocateBlock(sourceId: String, targetId: String, placement: BlockPlacement): TemplateDocument {
    fun find(items: List<PageBlock>, id: String): PageBlock? = items.firstNotNullOfOrNull { if (it.id == id) it else find(it.children, id) }
    val source = find(blocks, sourceId) ?: return this
    val target = find(blocks, targetId) ?: return this
    if (sourceId == targetId || find(source.children, targetId) != null) return this
    if (placement == BlockPlacement.INSIDE && target.kind !in listOf("GROUP", "TOGGLE")) return this
    fun insert(items: List<PageBlock>): List<PageBlock> = items.flatMap { item ->
        if (item.id != targetId) listOf(item.copy(children = insert(item.children)))
        else when (placement) {
            BlockPlacement.BEFORE -> listOf(source, item)
            BlockPlacement.AFTER -> listOf(item, source)
            BlockPlacement.INSIDE -> listOf(item.copy(children = item.children + source))
        }
    }
    return copy(blocks = insert(removeBlock(sourceId).blocks))
}

fun String.moveNoteLine(source: Int, target: Int, after: Boolean): String {
    val items = lines().toMutableList()
    if (source !in items.indices || target !in items.indices || source == target) return this
    val line = items.removeAt(source)
    val destination = target + (if (after) 1 else 0) - (if (source < target) 1 else 0)
    items.add(destination.coerceIn(0, items.size), line)
    return items.joinToString("\n")
}

fun String.setChecklistCompletion(checked: Boolean): String {
    val items = checklistItems().associateBy { it.lineIndex }
    return lines().mapIndexed { index, line ->
        if (items[index]?.let { it.checked != checked } == true) line.toggleChecklistLine(0) else line
    }.joinToString("\n")
}

fun TemplateDocument.setBoardStatus(tableId: String, rowId: String, fieldId: String, status: String): TemplateDocument {
    val table = table(tableId)
    val field = table.fields.firstOrNull { it.id == fieldId && it.kind == "CHOICE" } ?: return this
    if (status.isNotEmpty() && status !in field.options) return this
    return changeTable(tableId) { it.copy(rows = it.rows.map { row -> if (row.id == rowId && !row.archived) row.copy(values = row.values + (fieldId to status)) else row }) }
}

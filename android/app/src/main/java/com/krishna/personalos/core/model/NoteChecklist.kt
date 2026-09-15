package com.krishna.personalos.core.model

data class NoteChecklistItem(
    val lineIndex: Int,
    val label: String,
    val checked: Boolean,
)

private val checklistPattern = Regex("^(\\s*[-*+]\\s+)\\[([ xX])]\\s?(.*)$")

fun String.checklistItems(): List<NoteChecklistItem> =
    lines().mapIndexedNotNull { index, line ->
        checklistPattern.matchEntire(line)?.let { match ->
            NoteChecklistItem(index, match.groupValues[3], match.groupValues[2].equals("x", true))
        }
    }

fun String.checklistSummary(): String {
    val items = checklistItems()
    return if (items.isEmpty()) "No items" else "${items.count { it.checked }}/${items.size} complete"
}

fun String.toggleChecklistLine(lineIndex: Int): String = updateNoteLine(lineIndex) { line ->
    val match = checklistPattern.matchEntire(line) ?: return@updateNoteLine line
    val marker = if (match.groupValues[2].equals("x", true)) " " else "x"
    "${match.groupValues[1]}[$marker] ${match.groupValues[3]}"
}

fun String.renameChecklistLine(lineIndex: Int, label: String): String = updateNoteLine(lineIndex) { line ->
    val match = checklistPattern.matchEntire(line) ?: return@updateNoteLine line
    "${match.groupValues[1]}[${match.groupValues[2]}] $label"
}

fun String.replaceNoteLine(lineIndex: Int, replacement: String): String =
    lines().toMutableList().also { lines ->
        if (lineIndex in lines.indices) {
            lines.removeAt(lineIndex)
            lines.addAll(lineIndex, replacement.lines())
        }
    }.joinToString("\n")

fun String.removeNoteLine(lineIndex: Int): String =
    lines().filterIndexed { index, _ -> index != lineIndex }.joinToString("\n")

fun String.appendNoteLine(line: String): String = when {
    isEmpty() -> line
    endsWith("\n") -> this + line
    else -> "$this\n$line"
}

private fun String.updateNoteLine(lineIndex: Int, transform: (String) -> String): String =
    lines().mapIndexed { index, line -> if (index == lineIndex) transform(line) else line }.joinToString("\n")

package com.krishna.personalos.core.importing

import com.krishna.personalos.core.model.*
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class NotionImport(val title: String, val document: TemplateDocument, val warnings: List<String>) {
    fun selected(includeRecords: Boolean): TemplateDocument = if (includeRecords) document else document.copy(
        tables = document.tables.map { it.copy(rows = emptyList()) })
}

/** Reads data only: no extraction, URL requests, HTML execution or formula evaluation. */
object NotionImporter {
    const val MAX_BYTES = 10 * 1024 * 1024
    private fun id() = UUID.randomUUID().toString()
    fun read(name: String, input: InputStream): NotionImport {
        val files = linkedMapOf<String, String>()
        var total = 0
        var entries = 0
        var skipped = 0
        fun content(stream: InputStream): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BYTES) { "Export is too large. Import one page at a time (10 MB maximum)." }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
        if (name.endsWith(".zip", true)) {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++entries <= 500) { "Too many files. Export one template at a time (500 files maximum)." }
                    val path = entry.name.replace('\\', '/')
                    require(!path.startsWith('/') && ':' !in path && path.split('/').none { it == ".." }) { "Unsafe archive path." }
                    val bytes = content(zip)
                    if (!entry.isDirectory) {
                        if (path.endsWith(".md", true) || path.endsWith(".csv", true)) {
                            require(path !in files) { "Duplicate file name in archive." }
                            files[path] = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                        } else skipped++
                    }
                    zip.closeEntry()
                }
            }
        } else {
            require(name.endsWith(".md", true) || name.endsWith(".csv", true)) { "Choose a Notion Markdown & CSV ZIP, .md, or .csv file." }
            files[name] = content(input).toString(Charsets.UTF_8).removePrefix("\uFEFF")
        }
        require(files.isNotEmpty()) { "No Markdown or CSV files found. Export as Markdown & CSV from Notion." }
        val blocks = mutableListOf<PageBlock>()
        val tables = mutableListOf<PageTable>()
        var totalRows = 0
        var totalBlocks = 0
        files.entries.sortedWith(compareBy({ it.key.count { c -> c == '/' } }, { it.key })).forEach { (path, text) ->
            val title = cleanTitle(path)
            if (path.endsWith(".csv", true)) {
                val records = csv(text)
                require(records.isNotEmpty() && records.first().any { it.isNotBlank() }) { "$title: missing column names." }
                val headers = records.first()
                require(headers.size <= 100) { "A database may have at most 100 columns." }
                val data = records.drop(1).filterNot { it.all(String::isBlank) }
                require(data.size <= 5000 && data.all { it.size == headers.size }) { "$title: inconsistent columns or more than 5,000 rows." }
                totalRows += data.size
                require(totalRows <= 20000) { "Export contains too many records (20,000 maximum)." }
                // CSV cannot reliably describe Notion types/formulas; preserve exact text.
                val fields = headers.drop(1).mapIndexed { index, label -> PageField("column-${index + 1}", label.ifBlank { "Column ${index + 2}" }, "TEXT") }
                val table = PageTable(id(), title, fields, data.map { row -> PageRow(id(), row.first().ifBlank { "Untitled" }, fields.mapIndexed { index, field -> field.id to row[index + 1] }.toMap()) })
                tables += table
                blocks += PageBlock(id(), "TABLE", title, table.id, "matrix")
            } else {
                val children = text.lineSequence().filter(String::isNotBlank).map { line ->
                    require(++totalBlocks <= 20000) { "Export contains too many blocks (20,000 maximum)." }
                    val check = Regex("^\\s*[-*] \\[([ xX])]\\s*(.*)$").matchEntire(line)
                    when {
                        check != null -> PageBlock(id(), "CHECK", check.groupValues[2], checked = check.groupValues[1].equals("x", true))
                        line.startsWith('#') -> PageBlock(id(), "HEADING", line.trimStart('#', ' '))
                        line.startsWith("> ") -> PageBlock(id(), "QUOTE", line.removePrefix("> "))
                        else -> PageBlock(id(), "TEXT", line)
                    }
                }.toList()
                require(children.size <= 10000) { "Too many blocks in $title." }
                blocks += PageBlock(id(), "GROUP", title, children = children)
            }
        }
        val warnings = listOf("Offline copy, not live Notion sync. Layout, images, buttons, relations and formula logic are not recreated; CSV values stay editable text.") +
            if (skipped > 0) listOf("Skipped $skipped unsupported files (such as images or HTML).") else emptyList()
        return NotionImport(cleanTitle(name), TemplateDocument(blocks, tables), warnings)
    }

    private fun cleanTitle(path: String) = path.substringAfterLast('/').substringBeforeLast('.').replace(Regex(" [a-fA-F0-9]{32}$"), "").ifBlank { "Imported template" }

    internal fun csv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var closed = false
        var index = 0
        fun field() { row += cell.toString(); cell.setLength(0); closed = false; require(row.size <= 100) { "Too many columns." } }
        fun record() { field(); rows += row; row = mutableListOf(); require(rows.size <= 5001) { "Too many records." } }
        while (index < text.length) {
            val char = text[index++]
            if (quoted) {
                if (char == '"') {
                    if (index < text.length && text[index] == '"') { cell.append('"'); index++ }
                    else { quoted = false; closed = true }
                } else cell.append(char)
            } else when (char) {
                '"' -> { require(cell.isEmpty() && !closed) { "Invalid CSV quoting." }; quoted = true }
                ',' -> field()
                '\n', '\r' -> { if (char == '\r' && index < text.length && text[index] == '\n') index++; record() }
                else -> { require(!closed) { "Invalid characters after CSV quote." }; cell.append(char) }
            }
        }
        require(!quoted) { "Unclosed CSV quote." }
        if (cell.isNotEmpty() || row.isNotEmpty() || closed) record()
        return rows
    }
}

package com.krishna.personalos.core.importing

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NotionImporterTest {
    private fun archive(vararg files: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip -> files.forEach { (name, text) -> zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() } }
    }.toByteArray()

    @Test fun importsMarkdownAndCsvWithoutExecutingFormulas() {
        val zip = archive("Plan 12345678901234567890123456789012.md" to "# Plan\n- [x] Finished\n- [ ] Next\n> Reflect",
            "Plan/Tasks.csv" to "Name,Notes,Amount\r\nFirst,\"Line one\nLine two, with \"\"quotes\"\"\",=1+2\r\nSecond,,42")
        val result = NotionImporter.read("Plan.zip", zip.inputStream())
        assertEquals("Plan", result.title)
        assertEquals(2, result.document.blocks.size)
        assertTrue(result.document.blocks.first().children[1].checked)
        val table = result.document.tables.single()
        assertEquals("Line one\nLine two, with \"quotes\"", table.rows.first().values["column-1"])
        assertEquals("=1+2", table.rows.first().values["column-2"])
        assertTrue(table.fields.all { it.kind == "TEXT" })
        assertTrue(result.selected(false).tables.single().rows.isEmpty())
        assertEquals(2, result.selected(true).tables.single().rows.size)
    }

    @Test fun rejectsUnsafePaths() {
        assertThrows(IllegalArgumentException::class.java) { NotionImporter.read("export.zip", archive("../escape.md" to "No").inputStream()) }
    }
    @Test fun rejectsEmptyAndUnsupportedFiles() {
        assertThrows(IllegalArgumentException::class.java) { NotionImporter.read("export.html", "<h1>No</h1>".byteInputStream()) }
        assertThrows(IllegalArgumentException::class.java) { NotionImporter.read("export.zip", archive("image.png" to "data").inputStream()) }
    }
    @Test fun rejectsOversizedExportsEvenWhenCompressed() {
        assertThrows(IllegalArgumentException::class.java) { NotionImporter.read("large.zip", archive("large.md" to "x".repeat(NotionImporter.MAX_BYTES + 1)).inputStream()) }
    }
    @Test fun handlesBomEmptyCellsAndRejectsMalformedCsv() {
        val imported = NotionImporter.read("Table.csv", "\uFEFFName,Value\r\nFirst,\r\n".byteInputStream())
        assertEquals("", imported.document.tables.single().rows.single().values["column-1"])
        listOf("Name,Value\nFirst", "Name\n\"unclosed", "Name\n\"closed\"junk").forEach { text ->
            assertThrows(IllegalArgumentException::class.java) { NotionImporter.read("bad.csv", text.byteInputStream()) }
        }
    }
    @Test fun repeatedImportsHaveIndependentIdentifiers() {
        val first = NotionImporter.read("Data.csv", "Name,Value\nA,1".byteInputStream())
        val second = NotionImporter.read("Data.csv", "Name,Value\nA,1".byteInputStream())
        assertNotEquals(first.document.tables.single().id, second.document.tables.single().id)
        assertNotEquals(first.document.tables.single().rows.single().id, second.document.tables.single().rows.single().id)
    }
}

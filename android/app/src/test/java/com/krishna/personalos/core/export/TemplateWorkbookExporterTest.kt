package com.krishna.personalos.core.export

import com.krishna.personalos.core.model.PageField
import com.krishna.personalos.core.model.PageRow
import com.krishna.personalos.core.model.PageTable
import com.krishna.personalos.core.model.TemplateDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class TemplateWorkbookExporterTest {
    @Test fun workbookContainsMonthlyYearlyAndCompleteRecordSheets() {
        val document = TemplateDocument(emptyList(), listOf(
            PageTable(
                "expenses",
                "Expenses & Income",
                listOf(PageField("Date", "Date", "DATE"), PageField("Amount", "Amount", "NUMBER")),
                listOf(
                    PageRow("one", "Books", mapOf("Date" to "2026-01-10", "Amount" to "12.50")),
                    PageRow("two", "Travel", mapOf("Date" to "2026-02-11", "Amount" to "7.50"), archived = true),
                ),
            ),
        ))
        val output = ByteArrayOutputStream()

        TemplateWorkbookExporter.write("Finance & habits", document, "₹", output)

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        assertTrue("[Content_Types].xml" in entries)
        assertTrue("xl/worksheets/sheet1.xml" in entries)
        assertTrue(entries.getValue("xl/workbook.xml").contains("Monthly Summary"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Yearly Summary"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Expenses &amp; Income"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("2026-01"))
        assertFalse(entries.getValue("xl/worksheets/sheet1.xml").contains("2026-02"))
        assertFalse(entries.getValue("xl/worksheets/sheet2.xml").contains("20.00"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("Amount (₹)"))
        assertTrue(entries.getValue("xl/worksheets/sheet3.xml").contains("Travel"))
        assertEquals(10, entries.size)
    }

    @Test fun archivedRecordsCanBeExplicitlyIncludedInSummaryTotals() {
        val document = TemplateDocument(emptyList(), listOf(PageTable("expenses", "Expenses", emptyList(), listOf(
            PageRow("a", "Active", mapOf("Date" to "2026-01-01", "Amount" to "12.50")),
            PageRow("b", "Archived", mapOf("Date" to "2026-01-02", "Amount" to "7.50"), archived = true),
        ))))
        fun yearly(include: Boolean): String {
            val output = ByteArrayOutputStream()
            TemplateWorkbookExporter.write("Totals", document, "₹", output, include)
            ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/worksheets/sheet2.xml") return zip.readBytes().toString(Charsets.UTF_8)
                    entry = zip.nextEntry
                }
            }
            error("Missing yearly summary")
        }
        assertTrue(yearly(false).contains("12.50"))
        assertTrue(yearly(true).contains("20.00"))
    }
}

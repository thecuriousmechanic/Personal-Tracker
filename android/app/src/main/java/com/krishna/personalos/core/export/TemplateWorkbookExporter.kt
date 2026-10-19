package com.krishna.personalos.core.export

import com.krishna.personalos.core.model.PageRow
import com.krishna.personalos.core.model.TemplateDocument
import java.io.OutputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object TemplateWorkbookExporter {
    private const val CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun write(title: String, document: TemplateDocument, currencySymbol: String, output: OutputStream, includeArchived: Boolean = false) {
        val sheets = buildSheets(document, currencySymbol, includeArchived)
        ZipOutputStream(output.buffered()).use { zip ->
            zip.text("[Content_Types].xml", contentTypes(sheets.size))
            zip.text("_rels/.rels", rootRelationships())
            zip.text("xl/workbook.xml", workbook(sheets))
            zip.text("xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size))
            zip.text("xl/styles.xml", styles())
            sheets.forEachIndexed { index, sheet -> zip.text("xl/worksheets/sheet${index + 1}.xml", worksheet(sheet.rows)) }
            zip.text("docProps/core.xml", coreProperties(title))
            zip.text("docProps/app.xml", appProperties(sheets))
        }
    }

    private fun buildSheets(document: TemplateDocument, currencySymbol: String, includeArchived: Boolean): List<Sheet> {
        data class PeriodKey(val period: YearMonth, val table: String)
        val dated = document.tables.flatMap { table -> table.rows.filter { includeArchived || !it.archived }.mapNotNull { row -> row.month()?.let { Triple(it, table, row) } } }
        val monthly = dated.groupBy { PeriodKey(it.first, it.second.title.ifBlank { it.second.id }) }.toSortedMap(compareBy<PeriodKey> { it.period }.thenBy { it.table })
        val yearly = dated.groupBy { it.first.year to it.second.title.ifBlank { it.second.id } }.toSortedMap(compareBy<Pair<Int, String>> { it.first }.thenBy { it.second })
        val summaryHeader = "Amount ($currencySymbol)"
        val sheets = mutableListOf(
            Sheet("Monthly Summary", listOf(listOf(Cell("Month"), Cell("Table"), Cell("Records"), Cell(summaryHeader))) + monthly.map { (key, records) ->
                listOf(Cell(key.period.toString()), Cell(key.table), Cell(records.size.toString(), true), Cell(records.sumOf { it.third.amount() }.toPlainString(), true))
            }),
            Sheet("Yearly Summary", listOf(listOf(Cell("Year"), Cell("Table"), Cell("Records"), Cell(summaryHeader))) + yearly.map { (key, records) ->
                listOf(Cell(key.first.toString()), Cell(key.second), Cell(records.size.toString(), true), Cell(records.sumOf { it.third.amount() }.toPlainString(), true))
            }),
        )
        val usedNames = sheets.mapTo(mutableSetOf()) { it.name.lowercase(Locale.ROOT) }
        document.tables.forEach { table ->
            val name = uniqueSheetName(table.title.ifBlank { table.id }, usedNames)
            val header = listOf(Cell("Record"), Cell("Archived")) + table.fields.map { Cell(it.label.ifBlank { it.id }) }
            val rows = table.rows.map { row ->
                val month = row.month() ?: YearMonth.now()
                listOf(Cell(row.displayTitle()), Cell(if (row.archived) "Yes" else "No")) + table.fields.map { field ->
                    val value = document.displayValue(table, row, field, month)
                    Cell(value, field.kind == "NUMBER" && value.toBigDecimalOrNull() != null)
                }
            }
            sheets += Sheet(name, listOf(header) + rows)
        }
        return sheets
    }

    private fun PageRow.amount() = values["Amount"]?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO

    private fun PageRow.month(): YearMonth? {
        date()?.let { return YearMonth.from(it) }
        val titleMonth = runCatching { YearMonth.parse(title.trim(), DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)) }.getOrNull()
        if (titleMonth != null) return titleMonth
        val parts = values["Month Num"].orEmpty().split('-')
        if (parts.size == 2) {
            val month = parts[0].toIntOrNull()
            val year = parts[1].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
            if (month in 1..12 && year != null) return YearMonth.of(year, month!!)
        }
        return null
    }

    private fun uniqueSheetName(raw: String, used: MutableSet<String>): String {
        val base = raw.replace(Regex("[\\\\/*?:\\[\\]]"), " ").trim().ifBlank { "Records" }.take(31)
        var name = base
        var index = 2
        while (!used.add(name.lowercase(Locale.ROOT))) {
            val suffix = " $index"
            name = base.take(31 - suffix.length) + suffix
            index++
        }
        return name
    }

    private fun worksheet(rows: List<List<Cell>>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        append("<sheetFormatPr defaultRowHeight=\"15\"/><cols><col min=\"1\" max=\"128\" width=\"22\" customWidth=\"1\"/></cols><sheetData>")
        rows.forEachIndexed { rowIndex, cells ->
            val rowNumber = rowIndex + 1
            append("<row r=\"").append(rowNumber).append("\">")
            cells.forEachIndexed { columnIndex, cell ->
                val reference = columnName(columnIndex + 1) + rowNumber
                if (cell.numeric && cell.value.toBigDecimalOrNull() != null) {
                    append("<c r=\"").append(reference).append("\" s=\"").append(if (rowIndex == 0) 1 else 0).append("\"><v>").append(cell.value).append("</v></c>")
                } else {
                    append("<c r=\"").append(reference).append("\" t=\"inlineStr\" s=\"").append(if (rowIndex == 0) 1 else 0).append("\"><is><t xml:space=\"preserve\">")
                    append(xml(cell.value)).append("</t></is></c>")
                }
            }
            append("</row>")
        }
        append("</sheetData><autoFilter ref=\"A1:").append(columnName(rows.maxOfOrNull { it.size } ?: 1)).append("1\"/></worksheet>")
    }

    private fun columnName(number: Int): String {
        var value = number
        return buildString {
            while (value > 0) {
                value--
                insert(0, ('A'.code + value % 26).toChar())
                value /= 26
            }
        }
    }

    private fun contentTypes(sheetCount: Int) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"").append(CONTENT_TYPE).append(".main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        append("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/><Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>")
        (1..sheetCount).forEach { append("<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>") }
        append("</Types>")
    }

    private fun rootRelationships() = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>"""

    private fun workbook(sheets: List<Sheet>) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>")
        sheets.forEachIndexed { index, sheet -> append("<sheet name=\"").append(xml(sheet.name)).append("\" sheetId=\"").append(index + 1).append("\" r:id=\"rId").append(index + 1).append("\"/>") }
        append("</sheets></workbook>")
    }

    private fun workbookRelationships(sheetCount: Int) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        (1..sheetCount).forEach { append("<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>") }
        append("<Relationship Id=\"rId${sheetCount + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>")
    }

    private fun styles() = """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Aptos"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Aptos"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF0B1320"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""

    private fun coreProperties(title: String) = """<?xml version="1.0" encoding="UTF-8"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>${xml(title)}</dc:title><dc:creator>VECTOR OS</dc:creator></cp:coreProperties>"""

    private fun appProperties(sheets: List<Sheet>) = """<?xml version="1.0" encoding="UTF-8"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>VECTOR OS</Application><TitlesOfParts><vt:vector size="${sheets.size}" baseType="lpstr">${sheets.joinToString("") { "<vt:lpstr>${xml(it.name)}</vt:lpstr>" }}</vt:vector></TitlesOfParts></Properties>"""

    private fun xml(value: String): String = value.filter { it == '\t' || it == '\n' || it == '\r' || it.code >= 0x20 }
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun ZipOutputStream.text(path: String, value: String) {
        putNextEntry(ZipEntry(path).apply { time = 0L })
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private data class Sheet(val name: String, val rows: List<List<Cell>>)
    private data class Cell(val value: String, val numeric: Boolean = false)
}

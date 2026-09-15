package com.krishna.personalos.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.provider.OpenableColumns
import com.krishna.personalos.core.importing.*
import com.krishna.personalos.core.model.TemplateDocument
import kotlinx.coroutines.*

@Composable
internal fun NotionImportDialog(onDismiss: () -> Unit, onImport: suspend (String, TemplateDocument) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<NotionImport?>(null) }
    var title by remember { mutableStateOf("") }
    var includeRecords by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; error = null; preview = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    } ?: "export.zip"
                    context.contentResolver.openInputStream(uri)?.use { NotionImporter.read(name, it) } ?: error("Could not open this file.")
                }
                preview = result; title = result.title; includeRecords = false
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not read export." }
            finally { busy = false }
        }
    }
    Editor("Import from Notion", busy, error, onDismiss, footer = {
        Button(onClick = { val selected = preview ?: return@Button
            scope.launch {
                busy = true; error = null
                try { onImport(title.trim(), selected.selected(includeRecords)) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Import failed. Your existing templates were not changed." }
                finally { busy = false }
            }
        }, enabled = !busy && preview != null && title.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("confirm-notion-import")) { Text("Import template") }
    }) {
        Text("In Notion: ••• → Export → Markdown & CSV. Include subpages if needed, then choose the downloaded ZIP here. Individual .md and .csv files also work.")
        Text("Files stay on your device. Existing templates are never replaced. Maximum 10 MB uncompressed / 500 files.")
        OutlinedButton(onClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "text/*", "application/octet-stream")) }, enabled = !busy) { Text("Choose export file") }
        preview?.let { selected ->
            OutlinedTextField(title, { title = it }, enabled = !busy, label = { Text("Template name") }, modifier = Modifier.fillMaxWidth())
            Text("${selected.document.blocks.size} sections · ${selected.document.tables.size} databases · ${selected.document.tables.sumOf { it.rows.size }} available records")
            Row { Checkbox(includeRecords, { includeRecords = it }, enabled = !busy); Text("Include database records (off for an empty template)") }
            selected.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

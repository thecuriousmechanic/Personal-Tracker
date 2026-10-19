package com.krishna.personalos.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.app.NotificationManagerCompat
import com.krishna.personalos.core.database.TrashEntity
import com.krishna.personalos.core.export.WorkspaceBackup
import com.krishna.personalos.core.repository.RecoveryRepository
import com.krishna.personalos.ui.theme.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun WorkspaceSettingsScreen(
    insets: PaddingValues, darkMode: Boolean, onDarkMode: (Boolean) -> Unit,
    currencySymbol: String, onCurrencySymbol: (String) -> Unit,
    trash: List<TrashEntity>, recovery: RecoveryRepository?, backup: WorkspaceBackup?,
    onRestoreBackup: suspend (WorkspaceBackup.Preview) -> Unit,
    onBusyChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showTrash by rememberSaveable { mutableStateOf(false) }
    var currency by rememberSaveable(currencySymbol) { mutableStateOf(currencySymbol) }
    var preview by remember { mutableStateOf<WorkspaceBackup.Preview?>(null) }
    var eraseTarget by remember { mutableStateOf<TrashEntity?>(null) }
    val run: (suspend () -> Unit) -> Unit = { operation -> if (!busy) scope.launch {
        busy = true; onBusyChanged(true); error = null; message = null
        try { withContext(NonCancellable) { operation() } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Could not finish. Please try again." }
        finally { busy = false; onBusyChanged(false) }
    } }
    androidx.activity.compose.BackHandler(busy) {}
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && backup != null) run {
            withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "Could not open the selected file" }.use { backup.write(it) } }
            message = "Full workspace backup saved"
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && backup != null) run {
            preview = withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openInputStream(uri)) { "Could not open the selected backup" }.use { backup.preview(it) } }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        item {
            VectorHeader("Your space, safely kept", if (showTrash) "Trash" else "Settings") {
                if (showTrash) TextButton(onClick = { showTrash = false }, enabled = !busy) { Text("Back") }
            }
            if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp)); Text("Working…", color = Muted) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
            message?.let { Text(it, color = Cyan, modifier = Modifier.padding(vertical = 12.dp)) }
            Spacer(Modifier.height(24.dp))
        }
        if (!showTrash) {
            item {
                Text("APPEARANCE", style = MaterialTheme.typography.labelSmall, color = Cyan)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Dark appearance", color = Ice, style = MaterialTheme.typography.titleMedium); Text("A softer screen after hours", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    Switch(darkMode, onDarkMode, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Dark appearance" })
                }
                OutlinedTextField(currency, { currency = it.take(4) }, label = { Text("Currency symbol") }, supportingText = { Text("Used in finance totals and Excel exports") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                TextButton(onClick = { onCurrencySymbol(currency) }, enabled = !busy && currency.isNotBlank() && currency.none(Char::isISOControl) && currency != currencySymbol) { Text("Save currency") }
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 20.dp))
                Text("YOUR DATA", style = MaterialTheme.typography.labelSmall, color = Cyan)
                WorkspaceLine("Back up workspace", "Save every page, record, task, setting, and Trash item", onClick = { if (!busy && backup != null) export.launch("personal-tracker-backup-${LocalDate.now()}.json") })
                WorkspaceLine("Restore a backup", "Preview a backup before replacing this workspace", onClick = { if (!busy && backup != null) import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                WorkspaceLine("Trash", "${trash.size} recoverable ${if (trash.size == 1) "item" else "items"}", onClick = { if (!busy) showTrash = true })
                Text("Backups include your personal data. Keep the file somewhere you trust.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 20.dp))
                Text("REMINDERS", style = MaterialTheme.typography.labelSmall, color = Cyan)
                WorkspaceLine("Notification settings", if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "Notifications are allowed · enable reminders on individual tasks" else "Notifications are off · allow them for task reminders", onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) })
            }
        } else {
            if (trash.isEmpty()) item { Text("Nothing in Trash. Deleted pages and records will stay here until you restore or erase them.", color = Muted) }
            items(trash, key = { it.id }) { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    Text(item.title, color = Ice, style = MaterialTheme.typography.titleMedium)
                    Text(item.kind.replace('_', ' ') + " · " + Instant.ofEpochMilli(item.deletedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm")), color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { run { requireNotNull(recovery).restore(item.id); message = "Restored ${item.title}" } }, enabled = !busy) { Icon(Icons.Rounded.Restore, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Restore") }
                        TextButton(onClick = { eraseTarget = item }, enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Erase") }
                    }
                }
                HorizontalDivider(color = Divider)
            }
        }
    }
    preview?.let { selected ->
        AlertDialog(onDismissRequest = { if (!busy) preview = null }, title = { Text("Restore this workspace?") }, text = {
            Text("Backup created ${selected.exportedAt}\n${selected.records} stored records, including settings and Trash.\n\nThis replaces all current local data. Save a backup of this workspace first if you want to keep it.")
        }, confirmButton = { TextButton(onClick = { run { onRestoreBackup(selected); clearPageCoverCache(); preview = null; message = "Workspace restored" } }, enabled = !busy) { Text(if (busy) "Restoring…" else "Restore backup") } }, dismissButton = { TextButton(onClick = { preview = null }, enabled = !busy) { Text("Cancel") } })
    }
    eraseTarget?.let { item -> ConfirmDelete("item forever", "“${item.title}” will be removed from Trash. This cannot be undone.", busy, { eraseTarget = null }, { run { requireNotNull(recovery).erase(item.id); eraseTarget = null; message = "Item erased" } }) }
}

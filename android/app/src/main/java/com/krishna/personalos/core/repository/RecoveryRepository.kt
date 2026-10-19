package com.krishna.personalos.core.repository

import androidx.room.withTransaction
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.export.WorkspaceBackup
import com.krishna.personalos.core.model.*
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID

class RecoveryRepository(private val database: AppDatabase) {
    private val dao = database.recoveryDao()
    private val backup = WorkspaceBackup(database)
    val trash = dao.observeTrash()
    val preferences = dao.observePreferences()

    /** Caller captures and deletes inside the same Room transaction. */
    internal suspend fun capture(kind: String, title: String, ownerId: String, queries: Map<String, Pair<String, Array<out Any?>>>) {
        dao.saveTrash(TrashEntity(UUID.randomUUID().toString(), title, kind, ownerId, backup.snapshot(queries).toString(), System.currentTimeMillis()))
    }

    internal suspend fun captureRows(page: TemplatePageEntity, tableId: String, rows: List<PageRow>) {
        val fragment = TemplateDocument(emptyList(), listOf(TemplateDocument.parse(page.data).table(tableId).copy(rows = rows)))
        dao.saveTrash(TrashEntity(UUID.randomUUID().toString(), if (rows.size == 1) rows.first().displayTitle() else "${rows.size} records", "page_rows", page.id, fragment.toJson(), System.currentTimeMillis()))
    }

    internal suspend fun captureRemovedSections(page: TemplatePageEntity, before: TemplateDocument, after: TemplateDocument): Boolean {
        fun ids(items: List<PageBlock>): Set<String> = items.flatMap { listOf(it.id) + ids(it.children) }.toSet()
        val remaining = ids(after.blocks)
        val removed = mutableListOf<PageBlock>()
        val positions = JSONArray()
        fun visit(items: List<PageBlock>, parent: String?) {
            items.forEachIndexed { index, block ->
                if (block.id !in remaining) {
                    removed += block
                    positions.put(JSONObject().put("id", block.id).put("parent", parent ?: JSONObject.NULL).put("index", index))
                } else visit(block.children, block.id)
            }
        }
        visit(before.blocks, null)
        if (removed.isEmpty()) return false
        val payload = JSONObject().put("document", TemplateDocument(removed, emptyList()).toJson()).put("positions", positions)
        dao.saveTrash(TrashEntity(UUID.randomUUID().toString(), if (removed.size == 1) removed.first().text.ifBlank { "Section" } else "${removed.size} sections", "page_sections", page.id, payload.toString(), System.currentTimeMillis()))
        return true
    }

    suspend fun pin(id: String, pinned: Boolean) = dao.savePreference(WorkspacePreferenceEntity("pin:$id", pinned.toString()))
    suspend fun erase(id: String) = dao.removeTrash(id)
    suspend fun restore(id: String) = database.withTransaction {
        val item = requireNotNull(dao.trash(id)) { "This item is no longer in Trash" }
        if (item.kind == "page_sections") {
            val parent = requireNotNull(database.templatePageDao().page(item.ownerId)) { "Restore the page first" }
            require(!parent.deleted) { "Restore the page first" }
            val payload = JSONObject(item.payload)
            val sections = TemplateDocument.parse(payload.getString("document")).blocks
            val positions = payload.getJSONArray("positions")
            TemplateRepository(database).change(parent.id) { current ->
                fun contains(items: List<PageBlock>, id: String): Boolean = items.any { it.id == id || contains(it.children, id) }
                var restored = current
                sections.forEach { block ->
                    require(!contains(restored.blocks, block.id)) { "This section has already been restored" }
                    val position = (0 until positions.length()).map { positions.getJSONObject(it) }.first { it.getString("id") == block.id }
                    val parentId = position.optString("parent").takeIf { it.isNotBlank() && it != "null" && contains(restored.blocks, it) }
                    fun insert(items: List<PageBlock>) = items.toMutableList().apply { add(position.getInt("index").coerceIn(0, size), block) }.toList()
                    restored = if (parentId == null) restored.copy(blocks = insert(restored.blocks)) else restored.changeBlock(parentId) { it.copy(children = insert(it.children)) }
                }
                restored
            }
        } else if (item.kind == "page_rows") {
            val parent = requireNotNull(database.templatePageDao().page(item.ownerId)) { "Restore the page first" }
            require(!parent.deleted) { "Restore the page first" }
            val fragment = TemplateDocument.parse(item.payload)
            val current = TemplateDocument.parse(parent.data)
            require(current.tables.any { it.id == fragment.tables.single().id }) { "The original database was removed" }
            fragment.tables.single().rows.forEach { row -> TemplateRepository(database).saveRow(parent.id, fragment.tables.single().id, row) }
        } else {
            val tables = JSONObject(item.payload)
            backup.validate(tables, complete = false)
            // A restored project must not replace tasks edited since it was deleted.
            val linkedTasks = tables.optJSONArray("tasks")?.takeIf { item.kind == "project" }
            if (linkedTasks != null) tables.remove("tasks")
            if (item.kind == "tracker_entries") require(database.trackerDao().tracker(item.ownerId) != null) { "Restore the page first" }
            if (item.kind == "task") tables.optJSONArray("tasks")?.let { rows ->
                repeat(rows.length()) { index ->
                    val row = rows.getJSONObject(index)
                    if (!row.isNull("projectId") && database.appDao().project(row.getString("projectId")) == null) row.put("projectId", JSONObject.NULL)
                }
            }
            backup.insertRows(tables, replace = true)
            linkedTasks?.let { rows -> repeat(rows.length()) { index ->
                val original = rows.getJSONObject(index)
                database.appDao().task(original.getString("id"))?.takeIf { it.projectId == null }?.let { database.appDao().updateTask(it.copy(projectId = item.ownerId)) }
            } }
            if (item.kind == "page") TemplateDocumentCache.remove(item.ownerId)
        }
        dao.removeTrash(id)
        database.invalidationTracker.refreshAsync()
    }
}

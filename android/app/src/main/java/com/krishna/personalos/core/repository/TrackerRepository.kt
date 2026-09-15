package com.krishna.personalos.core.repository

import androidx.room.withTransaction
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import java.util.UUID
import kotlinx.coroutines.flow.combine

class TrackerRepository(private val database: AppDatabase) {
    private val dao = database.trackerDao()
    val workspace = combine(dao.observeTrackers(), dao.observeFields(), dao.observeEntries(), dao.observeCells(), ::TrackerWorkspace)
    private fun id() = UUID.randomUUID().toString()
    suspend fun delete(id: String) = database.withTransaction {
        dao.deleteCellsForTracker(id)
        dao.deleteEntriesForTracker(id)
        dao.deleteFieldsForTracker(id)
        dao.deleteTracker(id)
    }

    suspend fun create(starterId: String, title: String): String = database.withTransaction {
        require(title.isNotBlank()) { "Give your tracker a name" }
        val starter = TrackerStarters.all.first { it.id == starterId }
        val trackerId = id()
        dao.saveTracker(TrackerEntity(trackerId, title.trim(), starter.description, createdAt = System.currentTimeMillis()))
        dao.saveFields(starter.fields.mapIndexed { index, field ->
            TrackerFieldEntity(id(), trackerId, field.name, field.kind.name, field.options, index)
        })
        trackerId
    }

    suspend fun saveTracker(item: TrackerEntity) {
        require(item.title.isNotBlank()) { "Give your tracker a name" }
        dao.saveTracker(item.copy(title = item.title.trim()))
    }

    suspend fun saveField(field: TrackerFieldEntity) = database.withTransaction {
        require(dao.tracker(field.trackerId) != null) { "Tracker no longer exists" }
        require(field.name.isNotBlank()) { "Give the field a name" }
        val kind = FieldKind.valueOf(field.kind)
        val existing = dao.fields(field.trackerId)
        val previous = existing.find { it.id == field.id }
        require(previous == null || previous.kind == field.kind) { "Add a new field to use a different type" }
        require(existing.none { it.id != field.id && !it.archived && it.name.equals(field.name.trim(), true) }) { "That field name is already used" }
        if (kind == FieldKind.CHOICE) {
            require(field.choices().isNotEmpty()) { "Add at least one choice" }
            // Keep choices that are in use; otherwise an existing record becomes invalid.
            if (previous != null) require(field.choices().containsAll(previous.choices())) { "Existing choices must be kept; you can add more" }
        }
        dao.saveFields(listOf(field.copy(name = field.name.trim())))
    }

    suspend fun saveEntry(entry: TrackerEntryEntity, values: Map<String, String>) = database.withTransaction {
        require(entry.title.isNotBlank()) { "Give the entry a name" }
        require(dao.tracker(entry.trackerId) != null) { "Tracker no longer exists" }
        val previous = dao.entry(entry.id)
        require(previous == null || previous.trackerId == entry.trackerId)
        val fields = dao.fields(entry.trackerId).filter { !it.archived }
        fields.forEach { field -> require(field.valueError(values[field.id].orEmpty().trim()) == null) { "${field.name}: ${field.valueError(values[field.id].orEmpty().trim())}" } }
        dao.saveEntry(entry.copy(title = entry.title.trim()))
        dao.saveCells(fields.map { TrackerCellEntity(entry.id, it.id, values[it.id].orEmpty().trim()) })
    }

    suspend fun toggleCheckbox(entryId: String, fieldId: String) = database.withTransaction {
        val entry = requireNotNull(dao.entry(entryId))
        val field = dao.fields(entry.trackerId).first { it.id == fieldId && !it.archived }
        require(field.kind == FieldKind.CHECKBOX.name)
        val current = dao.cells(entryId).find { it.fieldId == fieldId }?.value == "true"
        dao.saveCells(listOf(TrackerCellEntity(entryId, fieldId, (!current).toString())))
    }

    suspend fun archiveEntry(entryId: String, archived: Boolean) = database.withTransaction {
        dao.entry(entryId)?.let { dao.saveEntry(it.copy(archived = archived)) }
    }

    suspend fun deleteEntry(entryId: String) = database.withTransaction {
        requireNotNull(dao.entry(entryId)) { "Entry no longer exists" }
        dao.deleteCells(entryId)
        dao.deleteEntry(entryId)
    }

    suspend fun deleteEntries(entryIds: Set<String>) = database.withTransaction {
        require(entryIds.isNotEmpty()) { "Select at least one entry" }
        val entries = entryIds.map { requireNotNull(dao.entry(it)) { "One or more entries no longer exist" } }
        require(entries.map { it.trackerId }.distinct().size == 1) { "Selected entries must belong to one tracker" }
        val ids = entryIds.toList()
        dao.deleteCells(ids)
        dao.deleteEntries(ids)
    }

    /** A clone owns its field and entry IDs; subsequent edits cannot affect its source. */
    suspend fun duplicate(sourceId: String, title: String, asTemplate: Boolean, includeEntries: Boolean): String = database.withTransaction {
        require(title.isNotBlank()) { "Give the copy a name" }
        val source = requireNotNull(dao.tracker(sourceId))
        val newId = id()
        dao.saveTracker(source.copy(id = newId, title = title.trim(), isTemplate = asTemplate, archived = false, createdAt = System.currentTimeMillis()))
        val fields = dao.fields(sourceId).filter { !it.archived }
        val fieldIds = fields.associate { it.id to id() }
        dao.saveFields(fields.map { it.copy(id = fieldIds.getValue(it.id), trackerId = newId) })
        if (includeEntries) dao.entries(sourceId).filter { !it.archived }.forEach { entry ->
            val entryId = id()
            dao.saveEntry(entry.copy(id = entryId, trackerId = newId))
            dao.saveCells(dao.cells(entry.id).filter { it.fieldId in fieldIds }.map { it.copy(entryId = entryId, fieldId = fieldIds.getValue(it.fieldId)) })
        }
        newId
    }
}

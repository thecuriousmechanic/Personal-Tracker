package com.krishna.personalos.core.model

import com.krishna.personalos.core.database.*

data class TrackerActions(
    val create: suspend (String, String) -> String,
    val saveTracker: suspend (TrackerEntity) -> Unit,
    val saveField: suspend (TrackerFieldEntity) -> Unit,
    val saveEntry: suspend (TrackerEntryEntity, Map<String, String>) -> Unit,
    val toggleCheckbox: suspend (String, String) -> Unit,
    val archiveEntry: suspend (String, Boolean) -> Unit,
    val deleteEntry: suspend (String) -> Unit,
    val deleteEntries: suspend (Set<String>) -> Unit,
    val duplicate: suspend (String, String, Boolean, Boolean) -> String,
    val delete: suspend (String) -> Unit,
)

package com.krishna.personalos.core.model

data class TemplateActions(
    val create: suspend (String, String, String) -> String,
    val change: suspend (String, (TemplateDocument) -> TemplateDocument) -> Unit,
    val rename: suspend (String, String, String) -> Unit,
    val archive: suspend (String, Boolean) -> Unit,
    val duplicate: suspend (String) -> String,
    val saveRow: suspend (String, String, PageRow) -> Unit,
    val deleteRow: suspend (String, String, String) -> Unit,
    val deleteRows: suspend (String, String, Set<String>) -> Unit,
    val delete: suspend (String) -> Unit,
    val importFinance: suspend () -> String = { error("Finance import is unavailable") },
    val importDocument: suspend (String, TemplateDocument) -> String = { _, _ -> error("Import unavailable") },
)

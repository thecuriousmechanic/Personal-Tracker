package com.krishna.personalos.core.model

import android.util.LruCache

/** Bounded parsed-page cache. getOrParse must be called off the UI thread. */
object TemplateDocumentCache {
    private data class Cached(val json: String, val document: TemplateDocument)
    private val cache = LruCache<String, Cached>(8)
    fun peek(id: String, json: String): TemplateDocument? = cache.get(id)?.takeIf { it.json == json }?.document
    fun latest(id: String): TemplateDocument? = cache.get(id)?.document
    fun put(id: String, json: String, document: TemplateDocument) { cache.put(id, Cached(json, document)) }
    fun getOrParse(id: String, json: String): TemplateDocument = peek(id, json) ?: TemplateDocument.parse(json).also { document -> document.tables.forEach { it.activeByDate }; cache.put(id, Cached(json, document)) }
    fun remove(id: String) { cache.remove(id) }
}

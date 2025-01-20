package org.data.caches

import kotlinx.serialization.json.JsonObject

/**
 * Simple cache manager for storing Wikidata claims and QIDs.
 */
object WikiCacheManager {
    private val claimCache = mutableMapOf<String, JsonObject>()
    private val qidCache = mutableMapOf<String, String>()

    fun getClaim(id: String): JsonObject? = claimCache[id]

    fun putClaim(id: String, entity: JsonObject) {
        claimCache[id] = entity
    }

    fun getQID(id: String): String? = qidCache[id]

    fun putQID(id: String, entity: String) {
        qidCache[id] = entity
    }
}

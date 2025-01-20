package org.data

import kotlinx.serialization.json.JsonObject

object ClaimCache {
    private val cache = mutableMapOf<String, JsonObject>()

    fun get(id: String): JsonObject? = cache[id]

    fun put(id: String, entity: JsonObject) {
        cache[id] = entity
    }
}

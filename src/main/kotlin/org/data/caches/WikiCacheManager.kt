package org.data.caches

import kotlinx.serialization.json.JsonObject

/** Simple cache manager for storing Wikidata claims and QIDs. */
object WikiCacheManager {
  /** QID to Claim. Used to reduce Wikidata queries. */
  private val claimCache = mutableMapOf<String, JsonObject>()

  /** Label to QID. Used to reduce Wikipedia queries. */
  private val qidCache = mutableMapOf<String, String>()

  /** QID to Label. Used to avoid backwards traversal of qidCache. */
  private val labelCache = mutableMapOf<String, String>()

  fun getClaim(id: String): JsonObject? = claimCache[id]

  fun putClaim(id: String, entity: JsonObject) {
    claimCache[id] = entity
  }

  fun getQID(id: String): String? = qidCache[id]

  fun putQID(id: String, entity: String) {
    qidCache[id] = entity
  }

  fun getLabel(id: String): String? = labelCache[id]

  fun putLabel(id: String, entity: String) {
    labelCache[id] = entity
  }
}

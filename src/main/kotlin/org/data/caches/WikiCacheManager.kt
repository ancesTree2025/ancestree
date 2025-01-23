package org.data.caches

import kotlinx.serialization.json.JsonObject
import org.data.models.*

/** Simple cache manager for storing Wikidata claims and QIDs. */
object WikiCacheManager {
  /** QID to Claim. Used to reduce Wikidata queries. */
  private val claimCache = mutableMapOf<QID, JsonObject>()

  /** Label to QID. Used to reduce Wikipedia queries. */
  private val qidCache = mutableMapOf<Label, QID>()

  /** QID to Label. Used to avoid backwards traversal of qidCache. */
  private val labelCache = mutableMapOf<QID, Label>()

  fun getClaim(id: QID): Claim? = claimCache[id]

  fun putClaim(id: QID, entity: Claim) {
    claimCache[id] = entity
  }

  fun getQID(id: Label): QID? = qidCache[id]

  fun putQID(id: Label, entity: QID) {
    qidCache[id] = entity
  }

  fun getLabel(id: QID): Label? = labelCache[id]

  fun putLabel(id: QID, entity: Label) {
    labelCache[id] = entity
  }
}

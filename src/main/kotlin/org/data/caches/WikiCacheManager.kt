package org.data.caches

import org.data.models.Label
import org.data.models.QID

/** Simple cache manager for storing Wikidata claims and QIDs. */
object WikiCacheManager {
  /** Label to QID. Used to reduce Wikipedia queries. */
  private val labelToQIDCache = mutableMapOf<Label, QID>()

  /** QID to Label. Used to avoid backwards traversal Label qidCache. */
  private val qidToLabelCache = mutableMapOf<QID, Label>()

  fun getQID(id: Label): QID? = labelToQIDCache[id]

  fun putQID(id: Label, entity: QID) {
    labelToQIDCache[id] = entity
  }

  fun getLabel(id: QID): Label? = qidToLabelCache[id]

  fun putLabel(id: QID, entity: Label) {
    qidToLabelCache[id] = entity
  }
}

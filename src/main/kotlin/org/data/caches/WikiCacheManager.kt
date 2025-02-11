package org.data.caches

import org.data.models.Label
import org.data.models.PropertyMapping
import org.data.models.QID
import org.data.models.Relations

/** Simple cache manager for storing Wikidata claims and QIDs. */
object WikiCacheManager {
  /** Label to QID. Used to reduce Wikipedia queries. */
  private val labelToQIDCache = mutableMapOf<Label, QID>()

  /** QID to Label. Used to avoid backwards traversal Label qidCache. */
  private val qidToLabelCache = mutableMapOf<QID, Label>()

  private val qidToPropsCache = mutableMapOf<QID, PropertyMapping>()

  fun getQID(id: Label): QID? = labelToQIDCache[id]

  fun putQID(id: Label, entity: QID) {
    labelToQIDCache[id] = entity
  }

  fun getLabel(id: QID): Label? = qidToLabelCache[id]

  fun putLabel(id: QID, entity: Label) {
    qidToLabelCache[id] = entity
  }

  fun getProps(id: QID): PropertyMapping? = qidToPropsCache[id]

  fun putProps(id: QID, entity: PropertyMapping) {
    qidToPropsCache[id] = entity
  }


}

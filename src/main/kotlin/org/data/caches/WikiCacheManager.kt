package org.data.caches

import org.data.models.Label
import org.data.models.Person
import org.data.models.PropertyMapping
import org.data.models.QID
import org.domain.models.Graph

/** Simple cache manager for storing Wikidata claims and QIDs. */
object WikiCacheManager {
  /** Label to QID. Used to avoid querying Wikipedia unnecessarily. */
  private val labelToQIDCache = mutableMapOf<Label, QID>()

  /** QID to Label. Used to avoid querying Wikidata unnecessarily. */
  private val qidToLabelCache = mutableMapOf<QID, Label>()

  /** QID to Claim. Used to avoid querying Wikidata unnecessarily. */
  private val qidToPropsCache = mutableMapOf<QID, PropertyMapping>()

  /** QID to a list of graphs. Used to determine relation links. */
  private val qidToGraphsCache = mutableMapOf<QID, MutableList<Graph<Person>>>()

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

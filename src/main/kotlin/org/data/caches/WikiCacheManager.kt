package org.data.caches

import org.data.models.Label
import org.data.models.Person
import org.data.models.PropertyMapping
import org.data.models.QID
import org.domain.models.Graph

interface WikiCacheManager {

  /*
  Label -> QID,
  QID -> Label,
  QID -> PropertyMapping,
  QID -> list of graphs,
   */

  /** Returns the QID for a given Label, or null if none is found online. */
  suspend fun getQID(id: Label): QID?

  fun putQID(id: Label, entity: QID)

  /** Returns the Label for a given QID, or null if none is found in cache. */
  fun getLabel(id: QID): Label?

  fun putLabel(id: QID, entity: Label)

  /** Returns the PropertyMapping for a given QID, or null if none is found in cache. */
  fun getProps(id: QID): PropertyMapping?

  fun putProps(id: QID, entity: PropertyMapping)

  /** Returns the graphs for a given QID, or null if none is found in cache. */
  fun getGraphs(id: QID): List<Graph<Person>>?

  fun putGraphs(id: QID, entity: Graph<Person>)
}

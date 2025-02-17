package org.data.caches

import org.data.models.Label
import org.data.models.PropertyMapping
import org.data.models.QID

interface WikiCacheManager {

  /*
  Label -> QID
  QID -> Label
  QID -> PropertyMapping
   */

  /** Returns the QID for a given Label, or null if none is found */
  fun getQID(id: Label): QID?

  fun putQID(id: Label, entity: QID)

  /** Returns the Label for a given QID, or null if none is found */
  fun getLabel(id: QID): Label?

  fun putLabel(id: QID, entity: Label)

  /** Returns the PropertyMapping for a given QID, or null if none is found */
  fun getProps(id: QID): PropertyMapping?

  fun putProps(id: QID, entity: PropertyMapping)
}

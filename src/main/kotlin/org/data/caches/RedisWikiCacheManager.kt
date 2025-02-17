package org.data.caches

import org.data.models.Label
import org.data.models.PropertyMapping
import org.data.models.QID

object RedisWikiCacheManager : WikiCacheManager {
  override suspend fun getQID(id: Label): QID? {
    TODO("Not yet implemented")
  }

  override fun putQID(id: Label, entity: QID) {
    TODO("Not yet implemented")
  }

  override fun getLabel(id: QID): Label? {
    TODO("Not yet implemented")
  }

  override fun putLabel(id: QID, entity: Label) {
    TODO("Not yet implemented")
  }

  override fun getProps(id: QID): PropertyMapping? {
    TODO("Not yet implemented")
  }

  override fun putProps(id: QID, entity: PropertyMapping) {
    TODO("Not yet implemented")
  }
}

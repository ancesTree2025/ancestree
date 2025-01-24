package org.data.caches

import org.data.models.*

object LabelCacheManager : CacheManager<QID, Label> {
  private val LabelCache = mutableMapOf<QID, Label>()

  override suspend fun get(key: QID): Label? {
    return LabelCache[key]
  }

  override suspend fun put(key: QID, value: Label) {
    LabelCache[key] = value
  }
}

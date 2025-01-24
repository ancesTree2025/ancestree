package org.data.caches

import org.data.models.*

object ClaimCacheManager : CacheManager<QID, Claim> {
  private val claimCache = mutableMapOf<QID, Claim>()

  override suspend fun get(key: QID): Claim? {
    return claimCache[key]
  }

  override suspend fun put(key: QID, value: Claim) {
    claimCache[key] = value
  }
}

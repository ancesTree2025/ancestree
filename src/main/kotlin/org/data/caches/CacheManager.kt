package org.data.caches

interface CacheManager<K, V> {
  suspend fun get(key: K): V?

  suspend fun put(key: K, value: V)
}

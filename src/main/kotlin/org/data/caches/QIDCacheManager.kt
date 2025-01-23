package org.data.caches

import org.data.models.*

object QIDCacheManager: CacheManager<Label, QID> {
    private val QIDCache = mutableMapOf<Label, QID>()

    override suspend fun get(key: Label): QID? {
        return QIDCache[key]
    }

    override suspend fun put(key: Label, value: QID) {
        QIDCache[key] = value
    }
}
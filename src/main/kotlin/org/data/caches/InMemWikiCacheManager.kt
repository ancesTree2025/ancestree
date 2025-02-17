package org.data.caches

import org.data.models.Label
import org.data.models.PropertyMapping
import org.data.models.QID
import org.data.parsers.WikiRequestParser
import org.data.requests.ComplexRequester

object InMemWikiCacheManager : WikiCacheManager {
  private val labelToQIDCache = mutableMapOf<Label, QID>()
  private val qidToLabelCache = mutableMapOf<QID, Label>()
  private val qidToPropsCache = mutableMapOf<QID, PropertyMapping>()

  override suspend fun getQID(id: Label): QID? {
    return labelToQIDCache.getOrElse(id) {
      // Make a query for id if not in cache
      val qidResp = ComplexRequester.searchWikidataForQID(id)
      val qid = WikiRequestParser.parseWikidataIDLookup(qidResp) ?: return null
      // add id -> qid map to cache
      putQID(id, qid)
      qid
    }
  }

  override fun putQID(id: Label, entity: QID) {
    labelToQIDCache[id] = entity
  }

  override fun getLabel(id: QID): Label? {
    return qidToLabelCache[id]
  }

  override fun putLabel(id: QID, entity: Label) {
    qidToLabelCache[id] = entity
  }

  override fun getProps(id: QID): PropertyMapping? {
    return qidToPropsCache[id]
  }

  override fun putProps(id: QID, entity: PropertyMapping) {
    qidToPropsCache[id] = entity
  }
}

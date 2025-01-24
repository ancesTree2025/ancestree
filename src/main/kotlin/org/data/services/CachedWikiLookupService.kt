package org.data.services

import io.ktor.server.plugins.*
import org.data.caches.ClaimCacheManager
import org.data.caches.LabelCacheManager
import org.data.caches.QIDCacheManager
import org.data.models.Label
import org.data.models.PersonAndFamilyInfo
import org.data.models.QID
import org.data.models.Relation
import org.data.parsers.parseClaimForFamily
import org.data.parsers.parseWikidataIDLookup
import org.data.parsers.parseWikidataQIDs
import org.data.requests.getLabelAndClaim
import org.data.requests.searchWikipediaForQID

/** Service class for performing Wikipedia/Wikidata lookups. */
class CachedWikiLookupService : LookupService<String, PersonAndFamilyInfo> {

  /**
   * The only exposed function, to be used for interaction with Wikipedia/Wikidata and for any sort
   * of querying.
   *
   * @param input The person's name.
   * @returns A 3-tuple of QID, Label and Family Relations.
   */
  override suspend fun query(input: String): PersonAndFamilyInfo {
    val qid = searchForPersonsQID(input)
    val labelAndFamily = getPersonsLabelAndFamilyMembers(qid)
    return PersonAndFamilyInfo(qid, labelAndFamily.first, labelAndFamily.second)
  }

  /**
   * Searches for a person and returns their QID. We first query the cache and then Wikipedia.
   *
   * @param name The person's name.
   * @returns Their Wikidata QID as a string.
   */
  private suspend fun searchForPersonsQID(name: String): QID {
    if (QIDCacheManager.get(name) != null) {
      return QIDCacheManager.get(name)!!
    }

    val response = searchWikipediaForQID(name)
    val qid = parseWikidataIDLookup(response)

    return qid
  }

  /**
   * Replaces QIDs in a family relation mapping with their labels.
   *
   * @param familyInfo A mapping of type of relation to a list of QIDs for individuals of that type.
   * @returns A human-readable mapping without QIDs.
   */
  private suspend fun replaceQIDsWithNames(familyInfo: Relation): Relation {
    /** We then select those names which don't appear in the cache, to query and store. */
    val allIds = familyInfo.values.flatten()
    val readableNames = mutableMapOf<String, String>()
    val toQuery = mutableListOf<String>()

    allIds.forEach { id ->
      val name = LabelCacheManager.get(id)
      if (name == null) {
        toQuery.add(id)
      } else {
        readableNames[id] = name
      }
    }

    /**
     * Finally, we retrieve the labels (as well as their claims for caching), and use them to map
     * the full list of IDs to names.
     */
    val idsParam = toQuery.joinToString("|")
    val nameResponse = getLabelAndClaim(idsParam)
    val labelClaimPair = parseWikidataQIDs(nameResponse)

    labelClaimPair.forEach { (qid, pair) ->
      readableNames[qid] = pair.first
      QIDCacheManager.put(pair.first, qid)
      ClaimCacheManager.put(qid, pair.second)
      LabelCacheManager.put(qid, pair.first)
    }

    return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
  }

  /**
   * Uses a person's QID to retrieve information about their family.
   *
   * @param personQID The person's QID.
   * @returns Their label and a mapping of types of relation to lists of relatives in that category.
   */
  private suspend fun getPersonsLabelAndFamilyMembers(personQID: QID): Pair<Label, Relation> {

    /** We first check if we have the claim stored in our cache to avoid a query. */
    if (ClaimCacheManager.get(personQID) == null) {

      /** If not, we query Wikidata to get them, and cache as relevant. */
      val familyResponse = getLabelAndClaim(personQID)
      val labelFamilyMap = parseWikidataQIDs(familyResponse)

      val labelFamilyPair =
        labelFamilyMap[personQID]
          ?: throw NotFoundException("Label Family pair not found in singleton map.")

      QIDCacheManager.put(labelFamilyPair.first, personQID)
      ClaimCacheManager.put(personQID, labelFamilyPair.second)
      LabelCacheManager.put(personQID, labelFamilyPair.first)
    }

    val familyInfo = replaceQIDsWithNames(parseClaimForFamily(ClaimCacheManager.get(personQID)!!))
    val label = LabelCacheManager.get(personQID)!!

    return Pair(label, familyInfo)
  }
}

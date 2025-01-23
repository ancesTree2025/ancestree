package org.data.services

import io.ktor.server.plugins.*
import org.data.models.PersonAndFamilyInfo
import org.data.parsers.parseClaimForFamily
import org.data.parsers.parseWikidataIDLookup
import org.data.parsers.parseWikidataQIDs
import org.data.requests.getLabelAndClaim
import org.data.requests.searchWikipediaForQID

/** Service class for performing Wikipedia/Wikidata lookups. */
class WikiLookupService : LookupService<String, PersonAndFamilyInfo> {

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
    return PersonAndFamilyInfo(
      qid,
      labelAndFamily.first,
      labelAndFamily.second,
    )
  }

  /**
   * Searches for a person and returns their QID. We first query the cache and then Wikipedia.
   *
   * @param name The person's name.
   * @returns Their Wikidata QID as a string.
   */
  private suspend fun searchForPersonsQID(name: String): String {
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
  private suspend fun replaceQIDsWithNames(
    familyInfo: Map<String, List<String>>
  ): Map<String, List<String>> {
    /** We then select those names which don't appear in the cache, to query and store. */
    val allIds = familyInfo.values.flatten()
    val readableNames = mutableMapOf<String, String>()

    val idsParam = allIds.joinToString("|")
    val nameResponse = getLabelAndClaim(idsParam)
    val labelClaimPair = parseWikidataQIDs(nameResponse)

    labelClaimPair.forEach { (qid, pair) ->
      readableNames[qid] = pair.first
    }

    return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
  }

  /**
   * Uses a person's QID to retrieve information about their family.
   *
   * @param personQID The person's QID.
   * @returns Their label and a mapping of types of relation to lists of relatives in that category.
   */
  private suspend fun getPersonsLabelAndFamilyMembers(
    personQID: String
  ): Pair<String, Map<String, List<String>>> {

    val familyResponse = getLabelAndClaim(personQID)
    val labelFamilyMap = parseWikidataQIDs(familyResponse)

    val labelFamilyPair =
      labelFamilyMap[personQID]
        ?: throw NotFoundException("Label Family pair not found in singleton map.")

    val label = labelFamilyPair.first
    val familyInfo = replaceQIDsWithNames(parseClaimForFamily(labelFamilyPair.second))

    return Pair(label, familyInfo)
  }
}

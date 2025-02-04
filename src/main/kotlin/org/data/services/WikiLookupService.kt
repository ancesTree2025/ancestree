package org.data.services

import io.ktor.server.plugins.*
import org.data.models.*
import org.data.parsers.WikiRequestParser
import org.data.requests.*

/** Service class for performing Wikipedia/Wikidata lookups. */
class WikiLookupService : LookupService<String, Pair<Person, NamedRelation>> {

  /**
   * The only exposed function, to be used for interaction with Wikipedia/Wikidata and for any sort
   * of querying.
   *
   * @param input The person's name.
   * @returns A 3-tuple of QID, Label and Family Relations.
   */
  override suspend fun query(input: String): Pair<Person, NamedRelation>? {
    val qid = searchForPersonsQID(input)

    if (qid.isNullOrEmpty()) {
      return null
    }

    val labelAndFamily =
      try {
        getPersonsLabelAndFamilyMembers(qid)
      } catch (e: NotFoundException) {
        return null
      }

    val personalInfo = NamedRelation.from(labelAndFamily.second)

    val person =
      Person(qid, labelAndFamily.first, labelAndFamily.second["Gender"]?.getOrNull(0) ?: "Unknown")

    return Pair(person, personalInfo)
  }

  /**
   * Searches for a person and returns their QID. We first query the cache and then Wikipedia.
   *
   * @param name The person's name.
   * @returns Their Wikidata QID as a string.
   */
  private suspend fun searchForPersonsQID(name: String): QID? {
    val response = ComplexRequester.searchWikidataForQID(name)
    val qid = WikiRequestParser.parseWikidataIDLookup(response)
    return qid
  }

  /**
   * Replaces QIDs in a family relation mapping with their labels.
   *
   * @param familyInfo A mapping of type of relation to a list of QIDs for individuals of that type.
   * @returns A human-readable mapping without QIDs.
   */
  private suspend fun replaceQIDsWithNames(familyInfo: PropertyMapping): PropertyMapping {
    /** We then select those names which don't appear in the cache, to query and store. */
    val allIds = familyInfo.values.flatten()

    if (allIds.isEmpty()) {
      return familyInfo
    }

    val readableNames = mutableMapOf<String, String>()

    val nameResponse = ComplexRequester.getLabelAndClaim(allIds)
    val labelClaimPair = WikiRequestParser.parseWikidataQIDs(nameResponse)

    labelClaimPair.forEach { (qid, pair) -> readableNames[qid] = pair.first }

    return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
  }

  /**
   * Uses a person's QID to retrieve information about their family.
   *
   * @param personQID The person's QID.
   * @returns Their label and a mapping of types of relation to lists of relatives in that category.
   */
  private suspend fun getPersonsLabelAndFamilyMembers(
    personQID: QID
  ): Pair<Label, PropertyMapping> {

    val familyResponse = ComplexRequester.getLabelAndClaim(listOf(personQID))
    val labelFamilyMap = WikiRequestParser.parseWikidataQIDs(familyResponse)

    val labelFamilyPair =
      labelFamilyMap[personQID]
        ?: throw NotFoundException("Label Family pair not found in singleton map.")

    val label = labelFamilyPair.first

    val familyInfo = replaceQIDsWithNames(labelFamilyPair.second)

    return Pair(label, familyInfo)
  }
}

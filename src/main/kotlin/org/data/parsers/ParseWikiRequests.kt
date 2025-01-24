package org.data.parsers

import io.ktor.client.statement.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.*
import org.data.models.*
import org.data.models.FamilyProperties.familyProps

/**
 * Parses Wikipedia ID Lookup responses, extracting the relevant QID.
 *
 * @param response The HTTP response from Wikipedia.
 * @returns A single parsed QID, as a string.
 */
suspend fun parseWikidataIDLookup(response: HttpResponse): QID? {
  val json = Json { ignoreUnknownKeys = true }
  val result = json.decodeFromString<PagesResponse>(response.bodyAsText())

  val qidSingleton = result.query?.pages?.values ?: return null

  val wikidataID =
    qidSingleton.toList()[0].pageprops?.wikibaseItem
      ?: throw NotFoundException(
        "Could not find Wikidata QID from pageprop in Wikipedia API search request."
      )

  return wikidataID
}

/**
 * Parses Wikidata entity lookup responses, caching claims and finding labels of all the relevant
 * family members.
 *
 * @param response The HTTP response from Wikidata.
 * @returns A mapping of QIDs to a pair of the name and the relation.
 */
suspend fun parseWikidataQIDs(response: HttpResponse): Map<QID, Pair<Label, Relation>> {
  val json = Json { ignoreUnknownKeys = true }
  val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

  return result.entities.mapValues { (_, entityInfo) ->
    val label = entityInfo.labels.en.value

    val familyInfo =
      familyProps.entries
        .associate { (key, value) ->
          value to
            (entityInfo.claims[key]?.flatMap { claim ->
              claim.mainsnak.datavalue?.value?.jsonObject?.get("id")?.jsonPrimitive?.content?.let {
                listOf(it)
              } ?: emptyList()
            } ?: emptyList())
        }
        .toMutableMap()

    familyProps.values.forEach { relation -> familyInfo.putIfAbsent(relation, emptyList()) }

    label to familyInfo
  }
}

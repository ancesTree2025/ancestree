package org.data.parsers

import io.ktor.client.statement.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.*
import org.data.models.DataLabel
import org.data.models.PagesResponse
import org.data.models.WikidataResponse

/**
 * Parses Wikipedia ID Lookup responses, extracting the relevant QID.
 *
 * @param response The HTTP response from Wikipedia.
 * @returns A single parsed QID, as a string.
 */
suspend fun parseWikidataIDLookup(response: HttpResponse): String {
  val json = Json { ignoreUnknownKeys = true }
  val result = json.decodeFromString<PagesResponse>(response.bodyAsText())

  val qidSingleton =
    result.query?.pages?.values
      ?: throw NotFoundException("Could not find search values from Wikipedia API request.")

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
 * @returns A mapping of types of relation to lists of names.
 */
suspend fun parseWikidataQIDs(response: HttpResponse): Map<String, Pair<String, JsonObject>> {
  val json = Json { ignoreUnknownKeys = true }
  val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

  val idToNameMap = mutableMapOf<String, Pair<String, JsonObject>>()

  result.entities.forEach { (id, entityInfo) ->
    val label = entityInfo.labels.en.value

    val claims = Json.parseToJsonElement(entityInfo.claims).jsonObject

    idToNameMap[id] = Pair(label, claims)
  }

  return idToNameMap
}

package org.data.parsers

import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import org.data.models.*
import org.data.models.WikidataProperties.propertyQIDMap

object WikiRequestParser {
  /**
   * Parses Wikipedia ID Lookup responses, extracting the relevant QID.
   *
   * @param response The HTTP response from Wikipedia.
   * @returns A single QID as a string, or null if none is found.
   */
  suspend fun parseWikidataIDLookup(response: HttpResponse): QID? {
    val json = Json { ignoreUnknownKeys = true }
    val result = json.decodeFromString<PagesResponse>(response.bodyAsText())

    val qidSingleton = result.query?.pages?.values ?: return null

    val wikidataID = qidSingleton.toList()[0].title

    return wikidataID
  }

  /**
   * Parses Wikidata entity lookup responses, caching claims and finding labels of all the relevant
   * family members.
   *
   * @param response The HTTP response from Wikidata.
   * @returns A mapping of QIDs to ist relations.
   */
  suspend fun parseWikidataClaims(
    response: HttpResponse,
    properties: Map<String, String> = propertyQIDMap,
  ): Map<QID, PropertyMapping> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.mapValues { (_, entityInfo) ->

      var familyInfo = mutableMapOf<String, List<String>>()

      familyInfo =
        properties.entries
          .associate { (key, value) ->
            value to
              (entityInfo.claims[key]?.flatMap { claim ->
                when (val dataValue = claim.mainsnak.datavalue?.value) {
                  is JsonObject -> {
                    when {
                      dataValue.containsKey("id") ->
                        listOf(dataValue["id"]!!.jsonPrimitive.content)
                      dataValue.containsKey("time") ->
                        listOf(dataValue["time"]!!.jsonPrimitive.content)
                      else -> emptyList()
                    }
                  }
                  is JsonPrimitive -> listOf(dataValue.content)
                  else -> emptyList()
                }
              } ?: emptyList())
          }
          .toMutableMap()

      propertyQIDMap.values.forEach { relation -> familyInfo.putIfAbsent(relation, emptyList()) }

      familyInfo
    }
  }

  suspend fun parseWikidataLabels(
    response: HttpResponse
  ): List<Label> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.map { (_, entityInfo) ->
      entityInfo.labels.en?.value ?: error("Label not found from entity query.")
    }
  }
}

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
   * Parses Wikidata claims for particular Q-items, returning the relevant properties as a property
   * map.
   *
   * @param response The HTTP response from Wikidata.
   * @param properties A map specifying properties of relevance, and their meaning.
   * @returns A mapping of QIDs to property maps determined by the passed properties.
   */
  suspend fun parseWikidataClaims(
    response: HttpResponse,
    properties: Map<String, String> = propertyQIDMap,
  ): Map<QID, PropertyMapping> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.mapValues { (_, entityInfo) ->
      val familyInfo: MutableMap<String, List<String>> =
        properties.entries
          .associate { (key, value) ->
            value to
              (entityInfo.claims[key]?.flatMap { claim ->
                when (val dataValue = claim.mainsnak.datavalue?.value) {
                  is JsonObject -> {
                    when {
                      dataValue.containsKey("id") -> listOf(dataValue["id"]!!.jsonPrimitive.content)
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

  /**
   * Parses Wikidata label for particular Q-items, returning a map of QID to label .
   *
   * @param response The HTTP response from Wikidata.
   * @returns A mapping of QIDs to labels.
   */
  suspend fun parseWikidataLabels(response: HttpResponse): Map<QID, Label> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.mapValues { (_, entityInfo) -> entityInfo.labels.en?.value ?: "Unknown" }
  }
}

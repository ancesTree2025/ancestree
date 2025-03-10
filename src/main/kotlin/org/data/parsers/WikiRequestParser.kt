package org.data.parsers

import io.ktor.client.statement.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

  suspend fun parseWikidataIDLookupMultiple(response: HttpResponse): List<QID>? {
    val json = Json { ignoreUnknownKeys = true }
    val result = json.decodeFromString<PagesResponse>(response.bodyAsText())

    val qidSingleton = result.query?.pages?.values ?: return null

    val wIDs = mutableListOf<QID>()

    qidSingleton.toList().forEach { wIDs.add(it.title) }

    return wIDs
  }

  private fun parseDataValue(jsonElement: JsonElement): List<String> {
    return when (jsonElement) {
      is JsonObject -> {
        when {
          jsonElement.containsKey("id") -> listOf(jsonElement["id"]!!.jsonPrimitive.content)
          jsonElement.containsKey("time") -> listOf(jsonElement["time"]!!.jsonPrimitive.content)
          jsonElement.containsKey("latitude") && jsonElement.containsKey("longitude") -> {
            val lat = jsonElement["latitude"]!!.jsonPrimitive.double
            val lon = jsonElement["longitude"]!!.jsonPrimitive.double
            listOf("$lat,$lon")
          }
          else -> emptyList()
        }
      }
      is JsonPrimitive -> listOf(jsonElement.content)
      else -> emptyList()
    }
  }

  private fun getQualifierValue(qualifierClaim: WikiClaim): String {
    return qualifierClaim.datavalue?.value?.let { parseDataValue(it).firstOrNull() ?: "Unknown" }
      ?: "Unknown"
  }

  private fun parseTime(time: String): String {
    val dateString = time.substringBefore("T").removePrefix("+")

    val fmtTime =
      try {
        LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("d/M/yyyy"))
      } catch (e: Throwable) {
        dateString.takeWhile { (it != '-') }
      }

    return fmtTime
  }

  private fun formatOfficeHeldClaim(claim: WikiClaim): String? {

    val qualifiers = claim.qualifiers ?: return null

    val roleId =
      claim.mainsnak?.datavalue?.value?.let { parseDataValue(it).firstOrNull() } ?: "Unknown Role"
    var p108 = qualifiers["P108"]?.firstOrNull()?.let { getQualifierValue(it) } ?: ""
    var p2389 = qualifiers["P2389"]?.firstOrNull()?.let { getQualifierValue(it) } ?: ""
    var p580 = qualifiers["P580"]?.firstOrNull()?.let { getQualifierValue(it) } ?: ""
    var p582 = qualifiers["P582"]?.firstOrNull()?.let { getQualifierValue(it) } ?: ""
    val p585 = qualifiers["P585"]?.firstOrNull()?.let { getQualifierValue(it) } ?: ""

    if (p108.isNotBlank()) {
      p108 = ", $p108"
    }

    if (p2389.isNotBlank()) {
      if (p2389 != p108) {
        p2389 = ", $p2389"
      } else {
        p2389 = ""
      }
    }

    p580 =
      if (p580.isNotBlank()) {
        parseTime(p580)
      } else {
        "Unknown"
      }

    p582 =
      if (p582.isNotBlank()) {
        parseTime(p582)
      } else {
        "Unknown"
      }

    if (p580 == "Unknown" && p582 == "Unknown" && p585.isNotBlank()) {
      return "$roleId$p108$p2389. ${parseTime(p585)}"
    } else {
      return "$roleId$p108$p2389. $p580 - $p582"
    }
  }

  suspend fun parseWikidataClaims(
    response: HttpResponse,
    properties: Map<String, String> = propertyQIDMap,
  ): Map<QID, PropertyMapping> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.mapValues { (_, entityInfo) ->
      val propertyMap = mutableMapOf<String, List<String>>()
      properties.entries.forEach { (prop, label) ->
        if (prop == "P39") {
          val formattedClaims =
            entityInfo.claims[prop]?.mapNotNull { claim -> formatOfficeHeldClaim(claim) }
              ?: emptyList()
          propertyMap[label] = formattedClaims
        } else {
          val values =
            entityInfo.claims[prop]?.flatMap { claim ->
              claim.mainsnak?.datavalue?.value?.let { parseDataValue(it) } ?: emptyList()
            } ?: emptyList()
          propertyMap[label] = values
        }
      }
      propertyMap
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

    return result.entities.mapValues { (_, entityInfo) ->
      entityInfo.labels["en"]?.value ?: entityInfo.labels["mul"]?.value ?: "Unknown"
    }
  }

  suspend fun parseWikiLinks(response: HttpResponse): Map<QID, String> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.mapValues { (_, entityInfo) ->
      entityInfo.sitelinks.enwiki?.url ?: "Unknown"
    }
  }

  suspend fun parseWikiDescriptions(response: HttpResponse): Map<QID, String> {
    val json = Json { ignoreUnknownKeys = true }

    val result = json.decodeFromString<WikidataResponse>(response.bodyAsText())

    return result.entities.mapValues { (_, entityInfo) ->
      entityInfo.descriptions["en"]?.value ?: "Unknown"
    }
  }
}

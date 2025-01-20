package org.data.parsers

import io.ktor.client.statement.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.*

import org.domain.models.PagesResponse

/**
 * Parses Wikipedia ID Lookup responses, extracting the relevant QID.
 *
 * @param response The HTTP response from Wikipedia.
 * @returns A single parsed QID, as a string.
 */
suspend fun parseWikidataIDLookup(response: HttpResponse) : String {
    val json = Json { ignoreUnknownKeys = true }
    val result = json.decodeFromString<PagesResponse>(response.bodyAsText())

    val qidSingleton = result.query?.pages?.values
        ?: throw NotFoundException("Could not find search values from Wikipedia API request.")

    val wikidataID = qidSingleton.toList()[0].pageprops?.wikibaseItem
        ?: throw NotFoundException("Could not find Wikidata QID from pageprop in Wikipedia API search request.")

    return wikidataID
}

/**
 * Parses Wikidata entity lookup responses, caching claims and finding labels of all the
 * relevant family members.
 *
 * @param response The HTTP response from Wikidata.
 * @returns A mapping of types of relation to lists of names.
 */
suspend fun parseWikidataQIDs(response: HttpResponse): Map<String, Pair<String, JsonObject>> {
    val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val entities = jsonResponse["entities"]?.jsonObject

    val idToNameMap = mutableMapOf<String, Pair<String, JsonObject>>()

    entities?.forEach { (id, details) ->
        val label =
            details.jsonObject["labels"]
                ?.jsonObject
                ?.get("en")
                ?.jsonObject
                ?.get("value")
                ?.jsonPrimitive
                ?.content
                ?: throw NotFoundException("Failed to parse entity label given QID. This should NEVER happen.")

        val claims =
            details.jsonObject["claims"]
                ?.jsonObject
                ?: throw NotFoundException("Failed to parse entity claims given QID. This should NEVER happen.")

        idToNameMap[id] = Pair(label, claims)
    }

    return idToNameMap
}
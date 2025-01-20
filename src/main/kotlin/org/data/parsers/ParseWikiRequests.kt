package org.data.parsers

import io.ktor.client.statement.*
import kotlinx.serialization.json.*

import org.data.caches.WikiCacheManager
import org.domain.models.PagesResponse

/**
 * Parses Wikipedia ID Lookup responses, extracting the relevant QID.
 *
 * @param response The HTTP response from Wikipedia.
 * @returns A single parsed QID, as a string.
 */
suspend fun parseWikidataIDLookup(response: HttpResponse) : String? {
    val json = Json { ignoreUnknownKeys = true }
    val result = json.decodeFromString<PagesResponse>(response.bodyAsText())
    val qidSingleton = result.query?.pages?.values?.toList()

    if (!qidSingleton.isNullOrEmpty()) {
        return qidSingleton[0].pageprops?.wikibaseItem ?: ""
    }
    return null
}

/**
 * Parses Wikidata claim lookup responses, extracting the relevant QIDs of all family members.
 *
 * @param response The HTTP response from Wikidata.
 * @returns A mapping of types of relation to lists of QIDs.
 */
suspend fun parseFamilyInfo(response: HttpResponse): Map<String, List<String>>? {
    val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val claims = json["claims"]?.jsonObject

    if (claims != null) {
        return parseClaimForFamily(claims)
    }
    return null

}

/**
 * Parses Wikidata entity lookup responses, caching claims and finding labels of all the
 * relevant family members.
 *
 * @param response The HTTP response from Wikidata.
 * @returns A mapping of types of relation to lists of names.
 */
suspend fun parseNames(response: HttpResponse): Map<String, Pair<String, JsonObject>> {
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
        idToNameMap[id] = Pair(label ?: "Unknown", details.jsonObject)
    }

    return idToNameMap
}
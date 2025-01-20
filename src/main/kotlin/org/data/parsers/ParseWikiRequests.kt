package org.data.parsers

import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import org.data.caches.ClaimCache
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
suspend fun parseFamilyInfo(response: HttpResponse): Map<String, List<String>> {
    val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val claims = json["claims"]?.jsonObject

    val familyProps =
        mapOf(
            "P22" to "Father",
            "P25" to "Mother",
            "P26" to "Spouse(s)",
            "P40" to "Child(ren)",
            "P3373" to "Sibling(s)"
        )

    val familyInfo = mutableMapOf<String, MutableList<String>>()

    claims?.forEach { (prop, claimDetails) ->
        if (prop in familyProps.keys) {
            val familyMembers =
                claimDetails.jsonArray.mapNotNull { claim ->
                    claim.jsonObject["mainsnak"]
                        ?.jsonObject
                        ?.get("datavalue")
                        ?.jsonObject
                        ?.get("value")
                        ?.jsonObject
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.content
                }
            if (familyMembers.isNotEmpty()) {
                familyInfo[familyProps[prop]!!] = familyMembers.toMutableList()
            }
        }
    }

    familyProps.values.forEach { relation -> familyInfo.putIfAbsent(relation, mutableListOf()) }

    return familyInfo.mapValues { it.value.toList() }
}

/**
 * Parses Wikidata entity lookup responses, caching claims and finding labels of all the
 * relevant family members.
 *
 * @param response The HTTP response from Wikidata.
 * @returns A mapping of types of relation to lists of names.
 */
suspend fun parseNames(response: HttpResponse): Map<String, String> {
    val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val entities = jsonResponse["entities"]?.jsonObject

    val idToNameMap = mutableMapOf<String, String>()

    entities?.forEach { (id, details) ->
        val label =
            details.jsonObject["labels"]
                ?.jsonObject
                ?.get("en")
                ?.jsonObject
                ?.get("value")
                ?.jsonPrimitive
                ?.content
        idToNameMap[id] = label ?: "Unknown"
        ClaimCache.put(id, details.jsonObject)
    }

    return idToNameMap
}
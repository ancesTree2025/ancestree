package org.data

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import org.domain.models.wiki.*

/**
 * Searches for a name in Wikipedia and returns Wikidata QID (if found).
 *
 * @param query The string to search for.
 * @param limit The number of results to return.
 * @return A Wikidata QID for the person being looked for.
 */
suspend fun searchWikipediaForQID(query: String, limit: Int = 1): String? {
    val response = doWikipediaRequest("query") {
        parameter("generator", "search")
        parameter("gsrsearch", query)
        parameter("gsrlimit", limit)
        parameter("prop", "pageprops")
        parameter("ppprop", "wikibase_item")
    }

    val json = Json { ignoreUnknownKeys = true }
    val result = json.decodeFromString<PagesResponse>(response.bodyAsText())
    val qidSingleton = result.query?.pages?.values?.toList()

    if (!qidSingleton.isNullOrEmpty()) {
        return qidSingleton[0].pageprops?.wikibaseItem ?: ""
    }

    return null
}


/**
 * Retrieves a list of family members for a person from Wikidata.
 *
 * @param wikidataId The wikidata ID of the person.
 * @returns A map of string to string, mapping the type of relation to the wikidata
 *   object of that person.
 */
suspend fun getFamilyInfo(wikidataId: String): Map<String, List<String>> {
    val response = doWikidataRequest("wbgetclaims") { parameter("entity", wikidataId) }

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
 * Converts Wikidata IDs to human-readable names.
 *
 * @param familyInfo A map of string to string, mapping the type of relation to the wikidata
 *   object of that person.
 * @returns A map of string to string, mapping the same as the above.
 */
suspend fun convertWikidataIdsToNames(
    familyInfo: Map<String, List<String>>
): Map<String, List<String>> {
    val allIds = familyInfo.values.flatten()
    if (allIds.isEmpty()) return familyInfo

    val idsParam = allIds.joinToString("|")

    val response =
        doWikidataRequest("wbgetentities") {
            parameter("ids", idsParam)
            parameter("props", "labels|claims")
            parameter("languages", "en")
        }

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

    return familyInfo.mapValues { (_, ids) -> ids.map { id -> idToNameMap[id] ?: "Unknown" } }
}

/**
 * Combines the above functions to return a map of family relationships for a person.
 *
 * @param name Name of the person being searched for.
 * @returns A map of string to string, mapping the type of relation to the wikidata
 *   object of that person.
 */
suspend fun fullQuery(name: String): Map<String, List<String>> {
    val wikidataId = searchWikipediaForQID(name)
    if (wikidataId == null) {
        println("No Wikidata ID found for $name")
        return emptyMap()
    }

    val familyInfo = getFamilyInfo(wikidataId)
    return convertWikidataIdsToNames(familyInfo)
}

/** Example function to sanity check. */
suspend fun main() {

    val personOfInterest = "Elon Musk"
    val familyMembers = fullQuery(personOfInterest)

    println("Family relationships for $personOfInterest:")
    familyMembers.forEach { (relation, names) -> println("$relation -> $names") }
}

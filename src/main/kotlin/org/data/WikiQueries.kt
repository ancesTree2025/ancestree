package org.data

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import org.domain.models.wiki.*

/**
 * Searches for a name in Wikipedia, and returns their page id.
 *
 * @param query - Name of the person being searched for.
 * @param limit - Number of search instances to return from Wikipedia.
 * @return searchItems - We return a list of search items as long as our limit, of potential matches
 *   for the query. These each have a page ID.
 */
suspend fun searchWikipediaArticles(query: String, limit: Int = 1): List<SearchItem> {
    val response =
        doWikipediaRequest("query") {
            parameter("list", "search")
            parameter("srsearch", query)
            parameter("srlimit", limit)
        }
    val json = Json { ignoreUnknownKeys = true }
    val jsonResponse = json.decodeFromString<Response>(response.body())
    return jsonResponse.query?.search.orEmpty()
}

/**
 * Uses Wikipedia page id to find the wiki-base Q-item code.
 *
 * @param pageId - A wikipedia page id for a person of interest.
 * @returns wikidataId - A wikidata QID (Q-item ID) tied to a specific person.
 */
suspend fun retrieveWikidataID(pageId: String): String? {
    val response =
        doWikipediaRequest("query") {
            parameter("prop", "pageprops")
            parameter("pageids", pageId)
        }

    val json = Json { ignoreUnknownKeys = true }
    val jsonResponse: WikiResponse = json.decodeFromString(response.bodyAsText())

    val page = jsonResponse.query?.pages?.values?.firstOrNull()
    return page?.pageprops?.get("wikibase_item")
}

/**
 * Retrieves a list of family members for a person from Wikidata.
 *
 * @param wikidataId - The wikidata ID of the person.
 * @returns familyInfo - A map of string to string, mapping the type of relation to the wikidata
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
 * @param familyInfo - A map of string to string, mapping the type of relation to the wikidata
 *   object of that person.
 * @returns familyInfo - A map of string to string, mapping the same as the above.
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
            parameter("props", "labels")
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
    }

    return familyInfo.mapValues { (_, ids) -> ids.map { id -> idToNameMap[id] ?: "Unknown" } }
}

/**
 * Combines the above functions to return a map of family relationships for a person.
 *
 * @param name - Name of the person being searched for.
 * @returns familyInfo - A map of string to string, mapping the type of relation to the wikidata
 *   object of that person.
 */
suspend fun fullQuery(name: String): Map<String, List<String>> {
    val pageId = searchWikipediaArticles(name).firstOrNull()?.pageid.toString()
    val wikidataId = retrieveWikidataID(pageId)
    if (wikidataId == null) {
        println("No Wikidata ID found for page $pageId")
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

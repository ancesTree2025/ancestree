package org.data.requests

import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * Searches for a name in Wikipedia and returns Wikidata QID (if found).
 *
 * @param query The string to search for.
 * @param limit The number of results to return.
 * @return HTTP Response.
 */
suspend fun searchWikipediaForQID(query: String, limit: Int = 1): HttpResponse {
    val response = doWikipediaRequest("query") {
        parameter("generator", "search")
        parameter("gsrsearch", query)
        parameter("gsrlimit", limit)
        parameter("prop", "pageprops")
        parameter("ppprop", "wikibase_item")
    }

    return response
}

/**
 * Retrieves a list of family members for a person from Wikidata.
 *
 * @param wikidataId The wikidata ID of the person.
 * @returns HTTP response.
 */
suspend fun getFamilyInfo(wikidataId: String): HttpResponse {
    val response = doWikidataRequest("wbgetclaims") { parameter("entity", wikidataId) }

    return response
}

/**
 * Converts Wikidata IDs to human-readable names.
 *
 * @param familyInfo A map of string to string, mapping the type of relation to the wikidata
 *   object of that person.
 * @returns HTTP response.
 */
suspend fun convertWikidataIdsToNames(qids: List<String>): HttpResponse {
    val idsParam = qids.joinToString("|")

    val response =
        doWikidataRequest("wbgetentities") {
            parameter("ids", idsParam)
            parameter("props", "labels|claims")
            parameter("languages", "en")
        }

    return response
}

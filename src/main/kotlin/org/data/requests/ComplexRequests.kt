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
  val response =
    doWikipediaRequest("query") {
      parameter("generator", "search")
      parameter("gsrsearch", query)
      parameter("gsrlimit", limit)
      parameter("prop", "pageprops")
      parameter("ppprop", "wikibase_item")
    }

  return response
}

/**
 * Converts Wikidata IDs to human-readable names.
 *
 * @param qids A string of bar-delimited (|) QIDs to retrieve labels and claims for.
 * @returns HTTP response.
 */
suspend fun getLabelAndClaim(qids: String): HttpResponse {
  val response =
    doWikidataRequest("wbgetentities") {
      parameter("ids", qids)
      parameter("props", "labels|claims")
      parameter("languages", "en")
    }

  return response
}

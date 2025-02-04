package org.data.requests

import io.ktor.client.request.*
import io.ktor.client.statement.*

object ComplexRequester {

  /**
   * Searches for a name in Wikidata and returns Wikidata QID (if found).
   *
   * @param query The string to search for.
   * @param limit The number of results to return.
   * @return HTTP Response.
   */
  suspend fun searchWikidataForQID(query: String, limit: Int = 1): HttpResponse {
    val response =
      BaseRequester.doWikidataRequest("query") {
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
   * @param qids A list of QIDs to retrieve labels and claims for.
   * @returns HTTP response.
   */
  suspend fun getLabelAndClaim(qids: List<String>): HttpResponse {
    val response =
      BaseRequester.doWikidataRequest("wbgetentities") {
        parameter("ids", qids.joinToString("|"))
        parameter("props", "labels|claims")
        parameter("languages", "en")
      }

    return response
  }
}

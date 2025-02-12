package org.data.requests

import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.data.models.QID

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
   * Gets the label and claims for particular QIDs passed in.
   *
   * @param qids A list of QIDs to retrieve labels or claims for.
   * @returns HTTP response.
   */
  suspend fun getLabelsOrClaims(qids: List<QID>): HttpResponse {
    val response =
      BaseRequester.doWikidataRequest("wbgetentities") {
        parameter("ids", qids.joinToString("|"))
        parameter("props", "labels|claims")
        parameter("languages", "en")
      }

    return response
  }

  suspend fun getAutocompleteNames(query: String, limit: Int = 5): HttpResponse {
    val apiKey = System.getenv("GOOGLE_API_KEY")
    val response =
      BaseRequester.doGoogleKnowledgeRequest {
        parameter("query", query)
        parameter("limit", limit)
        parameter("types", "Person")
        parameter("key", apiKey)
      }

    return response
  }
}

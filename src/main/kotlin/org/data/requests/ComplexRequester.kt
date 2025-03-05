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
   * Gets all the info for each QID passed in.
   *
   * @param qids A list of QIDs to retrieve info for.
   * @returns HTTP response.
   */
  suspend fun getInfo(qids: List<QID>): HttpResponse {
    val response =
      BaseRequester.doWikidataRequest("wbgetentities") {
        parameter("ids", qids.joinToString("|"))
        parameter("props", "claims|descriptions|labels|sitelinks/urls")
        parameter("languages", "en|mul")
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

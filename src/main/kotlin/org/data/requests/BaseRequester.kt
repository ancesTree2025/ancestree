package org.data.requests

import io.ktor.client.request.*
import io.ktor.client.statement.*

object BaseRequester {
  /** Simple enum with a list of the URLs we will query frequently. */
  enum class Endpoint(val baseUrl: String) {
    WIKIDATA("https://www.wikidata.org/w/api.php"),
    KGSEARCH("https://kgsearch.googleapis.com/v1/entities:search"),
  }

  /**
   * Template request function.
   *
   * @param endpoint The URL endpoint being queried.
   * @param configParams A list of query parameters to be passed.
   * @returns The HTTP response from the URL.
   */
  private suspend fun doRequest(
    endpoint: Endpoint,
    configParams: HttpRequestBuilder.() -> Unit,
  ): HttpResponse {
    val client = HttpClientProvider.httpClient
    return client.get(endpoint.baseUrl) { configParams() }
  }

  /**
   * Template request function for querying Wikidata.
   *
   * @param action The type of action to take (separated from params for distinction).
   * @param configParams A list of query parameters to be passed.
   * @returns The HTTP response from the URL.
   */
  suspend fun doWikidataRequest(
    action: String,
    configParams: HttpRequestBuilder.() -> Unit,
  ): HttpResponse {
    return doRequest(Endpoint.WIKIDATA) {
      parameter("action", action)
      parameter("format", "json")
      configParams()
    }
  }

  /**
   * Template request function for querying Google Knowledge Graph Search.
   *
   * @param configParams A list of query parameters to be passed.
   * @returns The HTTP response from the URL.
   */
  suspend fun doGoogleKnowledgeRequest(configParams: HttpRequestBuilder.() -> Unit): HttpResponse {
    return doRequest(Endpoint.KGSEARCH) { configParams() }
  }
}

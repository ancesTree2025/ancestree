package org.data.requests

import io.ktor.client.request.*
import io.ktor.client.statement.*

object BaseRequester {
  /** Interface for all endpoint enums. */
  interface Endpoint {
    val baseUrl: String
  }

  /** Simple enum with a list of the URLs we will query frequently. */
  enum class WikiEndpoint(override val baseUrl: String) : Endpoint {
    WIKIPEDIA("https://en.wikipedia.org/w/api.php"),
    WIKIDATA("https://www.wikidata.org/w/api.php"),
  }

  enum class GoogleEndPoint(override val baseUrl: String) : Endpoint {
    KGSEARCH("https://kgsearch.googleapis.com/v1/entities:search")
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
   * Template request function for querying Wikipedia.
   *
   * @param action The type of action to take (separated from params for distinction).
   * @param configParams A list of query parameters to be passed.
   * @returns The HTTP response from the URL.
   */
  suspend fun doWikipediaRequest( // TODO: unused?
    action: String,
    configParams: HttpRequestBuilder.() -> Unit,
  ): HttpResponse {
    return doRequest(WikiEndpoint.WIKIPEDIA) {
      parameter("action", action)
      parameter("format", "json")
      configParams()
    }
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
    return doRequest(WikiEndpoint.WIKIDATA) {
      parameter("action", action)
      parameter("format", "json")
      configParams()
    }
  }

  /**
   * Template request function for querying Google Knowledge Graph Search.
   *
   * @param action The type of action to take (separated from params for distinction).
   * @param configParams A list of query parameters to be passed.
   * @returns The HTTP response from the URL.
   */
  suspend fun doGoogleKnowledgeRequest(configParams: HttpRequestBuilder.() -> Unit): HttpResponse {
    return doRequest(GoogleEndPoint.KGSEARCH) { configParams() }
  }
}

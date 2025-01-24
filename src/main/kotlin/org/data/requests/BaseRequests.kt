package org.data.requests

import io.ktor.client.request.*
import io.ktor.client.statement.*

/** Simple enum with a list of the URLs we will query frequently. */
enum class WikiEndpoint(val baseUrl: String) {
  WIKIPEDIA("https://en.wikipedia.org/w/api.php"),
  WIKIDATA("https://www.wikidata.org/w/api.php"),
}

/**
 * Template request function.
 *
 * @param endpoint The URL endpoint being queried.
 * @param configParams A list of query parameters to be passed.
 * @returns The HTTP response from the URL.
 */
private suspend fun doRequest(
  endpoint: WikiEndpoint,
  configParams: HttpRequestBuilder.() -> Unit,
): HttpResponse {
  val client = HttpClientProvider.httpClient
  return client.get(endpoint.baseUrl) {
    configParams()
    parameter("format", "json")
  }
}

/**
 * Template request function for querying Wikipedia.
 *
 * @param action The type of action to take (separated from params for distinction).
 * @param configParams A list of query parameters to be passed.
 * @returns The HTTP response from the URL.
 */
suspend fun doWikipediaRequest(
  action: String,
  configParams: HttpRequestBuilder.() -> Unit,
): HttpResponse {
  return doRequest(WikiEndpoint.WIKIPEDIA) {
    parameter("action", action)
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
    configParams()
  }
}

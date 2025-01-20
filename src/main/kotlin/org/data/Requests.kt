package org.data

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

enum class WikiEndpoint(val baseUrl: String) {
  WIKIPEDIA("https://en.wikipedia.org/w/api.php"),
  WIKIDATA("https://www.wikidata.org/w/api.php")
}

// We use this class to avoid re-creating the HttpClient for each request.
object HttpClientProvider {
  val httpClient: HttpClient by lazy { HttpClient() }
}

suspend fun doRequest(
    endpoint: WikiEndpoint,
    configParams: HttpRequestBuilder.() -> Unit
): HttpResponse {
  val client = HttpClientProvider.httpClient
  return client.get(endpoint.baseUrl) {
    configParams()
    parameter("format", "json")
  }
}

suspend fun doWikipediaRequest(
    action: String,
    configParams: HttpRequestBuilder.() -> Unit
): HttpResponse {
  return doRequest(WikiEndpoint.WIKIPEDIA) {
    parameter("action", action)
    configParams()
  }
}

suspend fun doWikidataRequest(
    action: String,
    configParams: HttpRequestBuilder.() -> Unit
): HttpResponse {
  return doRequest(WikiEndpoint.WIKIDATA) {
    parameter("action", action)
    configParams()
  }
}

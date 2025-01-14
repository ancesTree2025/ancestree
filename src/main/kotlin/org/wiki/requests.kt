package org.wiki

import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.json.Json


@Serializable
data class SearchResult(
    val ns: Int,
    val title: String,
    val pageid: Int
)

@Serializable
data class SearchQuery(
    val search: List<SearchResult>
)

@Serializable
data class SearchResponse(
    val query: SearchQuery? = null
)

suspend fun searchWikipediaArticles(
    query: String,
    limit: Int = 1
): List<SearchResult> {
    // Initialize the Ktor client
    val client = HttpClient {
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    // Wikipedia search API endpoint
    val url = "https://en.wikipedia.org/w/api.php"

    // Perform GET request
    val response: HttpResponse = client.get(url) {
        parameter("action", "query")
        parameter("list", "search")
        parameter("srsearch", query)
        parameter("srlimit", limit)
        parameter("format", "json")
    }

    // Parse the response body into our data classes
    val responseBody: String = response.body()
    val searchResponse = Json.decodeFromString<SearchResponse>(responseBody)

    // Clean up the client
    client.close()

    return searchResponse.query?.search.orEmpty()
}

suspend fun main() {
    val results = searchWikipediaArticles("Elizabeth II", limit = 1)
    if (results.isNotEmpty()) {
        println("First result title: ${results[0].title}")
    } else {
        println("No results found.")
    }
}

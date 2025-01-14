package org.wiki

import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.json.Json

@Serializable
data class SearchItem(
    val title: String,
    val pageid: Int
)

@Serializable
data class Query(
    val search: List<SearchItem> = emptyList()
)

@Serializable
data class Response(
    val query: Query? = null
)


suspend fun searchWikipediaArticles(
    query: String,
    limit: Int = 1):
        List<SearchItem> {

    val client = HttpClient {
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    val url = "https://en.wikipedia.org/w/api.php"

    val response: HttpResponse = client.get(url) {
        parameter("action", "query")
        parameter("list", "search")
        parameter("srsearch", query)
        parameter("srlimit", limit)
        parameter("format", "json")
    }

    val json = Json { ignoreUnknownKeys = true }
    val jsonResponse = json.decodeFromString<Response>(response.body())
    val searchItems = jsonResponse.query?.search.orEmpty()
    return searchItems;
}

suspend fun main() {
    val results = searchWikipediaArticles("Elizabeth II", limit = 1)
    println(results)
}

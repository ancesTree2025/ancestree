package org.wiki

import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*


/* The classes below are used to extract the information made in HTTP requests.
* The first 3 are primarily used when querying wikipedia, and the last 3 are
* primarily used when querying wikidata. */
@Serializable
data class SearchItem( val title: String, val pageid: Int )

@Serializable
data class Query( val search: List<SearchItem> = emptyList() )

@Serializable
data class Response( val query: Query? = null )

@Serializable
data class WikiPageProps( val pageprops: Map<String, String>? = null )

@Serializable
data class WikiQuery( val pages: Map<String, WikiPageProps> )

@Serializable
data class WikiResponse( val query: WikiQuery? = null )

/* This function is responsible for creating a client and using it to query sites, and return
* the HTTP response.
* @param configParams   - A HTTP request builder with pre-determined parameters from different functions.
* @param action         - States the type of action in the query. ALso used to determine which of wikipedia
*                         and wikidata to query. The default is set to query, as a few other functions use this.
* @return response      - Returns the HTTP response sent from Wikipedia, */
suspend fun generateClient(configParams: HttpRequestBuilder.() -> Unit,
                           action: String = "query") : HttpResponse {
    val client = HttpClient()

    var url = "https://en.wikipedia.org/w/api.php"
    if (action != "query"){
        url = "https://www.wikidata.org/w/api.php"
    }

    val response: HttpResponse = client.get(url) {
        parameter("action", action)
        configParams()
        parameter("format", "json")
    }

    client.close()

    return response
}

/* This function searches for a particular name in Wikipedia, and creates an object with the
* title of that person and their page ID.
* @param query          - Name of the person being searched for.
* @param limit          - Number of search instances to return from Wikipedia.
* @return searchItems   - We return a list of search items as long as our limit, of potential
*                         matches for the query. These each have a page ID.*/
suspend fun searchWikipediaArticles(query: String, limit: Int = 1): List<SearchItem> {

    val customParams: HttpRequestBuilder.() -> Unit = {
        parameter("list", "search")
        parameter("srsearch", query)
        parameter("srlimit", limit)
    }

    val response = generateClient(customParams)

    val json = Json { ignoreUnknownKeys = true }
    val jsonResponse = json.decodeFromString<Response>(response.body())

    val searchItems = jsonResponse.query?.search.orEmpty()
    return searchItems;
}

/* This function is used after searchWikipediaArticles, and uses the page id to find the wiki-base
* item. This is crucial as we will then use this in wikidata to find out more family information.
* @param pageId         - A wikipedia page id for a person of interest.
* @returns wikidataId   - A wikidata QID (Q-item ID) tied to a specific person. */
suspend fun retrieveWikidataID(pageId: String): String? {

    val customParams: HttpRequestBuilder.() -> Unit = {
        parameter("prop", "pageprops")
        parameter("pageids", pageId)
    }

    val response = generateClient(customParams)

    val json = Json { ignoreUnknownKeys = true }
    val jsonResponse: WikiResponse = json.decodeFromString(response.bodyAsText())

    val page = jsonResponse.query?.pages?.values?.firstOrNull()
    val wikidataId = page?.pageprops?.get("wikibase_item")

    return wikidataId
}

suspend fun getFamilyInfo(wikidataId: String): Map<String, String> {
    val customParams: HttpRequestBuilder.() -> Unit = {
        parameter("entity", wikidataId)
    }

    val response = generateClient(customParams, "wbgetclaims")

    val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val claims = jsonResponse["claims"]?.jsonObject

    val familyProps = mapOf("P22" to "father", "P25" to "mother", "P26" to "spouse")
    val familyInfo = mutableMapOf<String, String>()

    claims?.forEach { (prop, claimDetails) ->
        if (prop in familyProps.keys) {
            val familyMemberId = claimDetails.jsonArray[0]
                .jsonObject["mainsnak"]?.jsonObject?.get("datavalue")?.jsonObject
                ?.get("value")?.jsonObject?.get("id")?.jsonPrimitive?.content

            familyInfo[familyProps[prop]!!] = familyMemberId ?: "Unknown"
        }
    }

    return familyInfo
}

suspend fun convertWikidataIdsToNames(ids: List<String>): Map<String, String> {

    val idsParam = ids.joinToString("|")

    val customParams: HttpRequestBuilder.() -> Unit = {
        parameter("ids", idsParam)
        parameter("props", "labels")
        parameter("languages", "en")
    }

    val response = generateClient(customParams, "wbgetentities")

    val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val entities = jsonResponse["entities"]?.jsonObject

    val namesMap = mutableMapOf<String, String>()

    entities?.forEach { (id, entityDetails) ->
        val label = entityDetails.jsonObject["labels"]?.jsonObject?.get("en")?.jsonObject
            ?.get("value")?.jsonPrimitive?.content
        if (label != null) {
            namesMap[id] = label
        } else {
            namesMap[id] = "Unknown" // Fallback for missing labels
        }
    }

    return namesMap
}

suspend fun main() {

    val pageId = searchWikipediaArticles("Alexander the Great").first().pageid.toString()
    println(pageId)
    val wikidataId = retrieveWikidataID(pageId) ?: return
    println(wikidataId)
    val familyInfo = getFamilyInfo(wikidataId)
    println(familyInfo)
    val familyMemberIds = familyInfo.values.toList()
    println(familyMemberIds)
    val familyMemberNames = convertWikidataIdsToNames(familyMemberIds)
    println(familyMemberNames)

}


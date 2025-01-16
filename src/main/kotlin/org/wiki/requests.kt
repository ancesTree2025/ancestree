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
data class SearchItem( val pageid: Int )

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

/* After retrieving the wikidata object ID we can then use this function to retrieve the
* data stored about it. This returns a huge amount of information, from which we extract
* the direct relatives of the person.
*
* The @var familyInfo is a map of strings to mutable lists - these strings will become
* the descriptors for the properties which we find in the response, such as "father"
* and "spouse". The lists of strings that they are mapping to will be lists of wikidata
* q-item IDs.
*
* @param wikidataId     - The wikidata ID of the person.
* @returns familyInfo   - A map of string to string, mapping the type of relation to the
*                         wikidata object of that person. */
suspend fun getFamilyInfo(wikidataId: String): Map<String, List<String>> {
    val customParams: HttpRequestBuilder.() -> Unit = {
        parameter("entity", wikidataId)
    }

    // As in the previous queries, we set up our HTTP requests as before.
    val response = generateClient(customParams, "wbgetclaims")

    val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val claims = jsonResponse["claims"]?.jsonObject

    val familyProps = mapOf("P22" to "Father", "P25" to "Mother", "P26" to "Spouse(s)", "P40" to "Child(ren)")
    val familyInfo = mutableMapOf<String, MutableList<String>>()

    // For each property that appears in the response's list of claims, if a relevant
    // property appears, we look through the claim details and extract the objects/values
    // from there.
    claims?.forEach { (prop, claimDetails) ->
        if (prop in familyProps.keys) {
            val familyMembers = claimDetails.jsonArray.mapNotNull { claim ->
                claim.jsonObject["mainsnak"]?.jsonObject?.get("datavalue")?.jsonObject
                    ?.get("value")?.jsonObject?.get("id")?.jsonPrimitive?.content
            }

            if (familyMembers.isNotEmpty()) {
                familyInfo[familyProps[prop]!!] = familyMembers.toMutableList()
            }
        }
    }

    // If a particular property did not appear, we initialise it but keep it empty.
    familyProps.values.forEach { key ->
        familyInfo.putIfAbsent(key, mutableListOf())
    }

    return familyInfo
}

/* The final function in the list of calls simply converts all the wikidata object IDs to
* names. This is done for each list of relevant people.
* @param familyInfo     - A map of string to string, mapping the type of relation to the
*                         wikidata object of that person.
* @*/
suspend fun convertWikidataIdsToNames(familyInfo: Map<String, List<String>>): Map<String, List<String>> {
    val allIds = familyInfo.values.flatten()

    val idsParam = allIds.joinToString("|")

    val customParams: HttpRequestBuilder.() -> Unit = {
        parameter("ids", idsParam)
        parameter("props", "labels")
        parameter("languages", "en")
    }

    val response = generateClient(customParams, "wbgetentities")

    val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    val entities = jsonResponse["entities"]?.jsonObject

    val idToNameMap = mutableMapOf<String, String>()

    // For each entity in the response, we extract the label of the entity and map it to the
    // object ID.
    entities?.forEach { (id, entityDetails) ->
        val label = entityDetails.jsonObject["labels"]?.jsonObject?.get("en")?.jsonObject
            ?.get("value")?.jsonPrimitive?.content
        idToNameMap[id] = label ?: "Unknown"
    }

    val result = mutableMapOf<String, List<String>>()

    familyInfo.forEach { (relationshipType, ids) ->
        val namesList = ids.mapNotNull { id -> idToNameMap[id] }
        result[relationshipType] = namesList
    }

    return result
}

suspend fun main() {

    val pageId = searchWikipediaArticles("Edward of Windsor").first().pageid.toString()
    val wikidataId = retrieveWikidataID(pageId) ?: return
    val familyInfo = getFamilyInfo(wikidataId)
    val familyMemberNames = convertWikidataIdsToNames(familyInfo)
    println(familyMemberNames)

}


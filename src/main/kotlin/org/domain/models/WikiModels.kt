package org.domain.models.wiki

import kotlinx.serialization.Serializable

/**
 * The classes below are used to extract the information made in HTTP requests. The first 3 are
 * primarily used when querying wikipedia, and the last 3 are primarily used when querying wikidata.
 */
@Serializable data class SearchItem(val pageid: Int)

@Serializable data class Query(val search: List<SearchItem> = emptyList())

@Serializable data class Response(val query: Query? = null)

@Serializable data class WikiPageProps(val pageprops: Map<String, String>? = null)

@Serializable data class WikiQuery(val pages: Map<String, WikiPageProps>)

@Serializable data class WikiResponse(val query: WikiQuery? = null)

package org.domain.models.wiki

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The classes below are used to extract the information made in HTTP requests. The first 3 are
 * primarily used when querying wikipedia, and the last 3 are primarily used when querying wikidata.
 */
@Serializable data class PagesResponse(val query: PagesQuery? = null)

@Serializable data class PagesQuery(val pages: Map<String, PageInfo>? = null)

@Serializable data class PageInfo(val pageprops: PageProps? = null)

@Serializable data class PageProps(@SerialName("wikibase_item") val wikibaseItem: String? = null)

@Serializable data class WikiPageProps(val pageprops: Map<String, String>? = null)

@Serializable data class WikiQuery(val pages: Map<String, WikiPageProps>)

@Serializable data class WikiResponse(val query: WikiQuery? = null)

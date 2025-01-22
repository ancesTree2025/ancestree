package org.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The classes below are used to extract the information made in HTTP requests. */
@Serializable data class PagesResponse(val query: PagesQuery? = null)

@Serializable data class PagesQuery(val pages: Map<String, PageInfo>? = null)

@Serializable data class PageInfo(val pageprops: PageProps? = null)

@Serializable data class PageProps(@SerialName("wikibase_item") val wikibaseItem: String? = null)

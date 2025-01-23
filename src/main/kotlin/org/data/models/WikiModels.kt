package org.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The classes below are used to extract the information made in HTTP requests. */
@Serializable data class PagesResponse(val query: PagesQuery? = null)

@Serializable data class PagesQuery(val pages: Map<String, PageInfo>? = null)

@Serializable data class PageInfo(val pageprops: PageProps? = null)

@Serializable data class PageProps(@SerialName("wikibase_item") val wikibaseItem: String? = null)

@Serializable data class WikidataResponse(val entities: Map<String, EntityInfo>)

@Serializable data class EntityInfo(val labels: LangInfo, val claims: Map<String, String>)

@Serializable data class LangInfo(val en: DataLabel)

@Serializable data class DataLabel(val value: String)


package org.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/* The classes below are used to extract the information made in HTTP requests. */

/** Response from making a query on a name to Wikidata */
@Serializable data class PagesResponse(val query: PagesQuery? = null)

@Serializable data class PagesQuery(val pages: Map<String, PageInfo>? = null)

@Serializable data class PageInfo(val title: String)

/** Wikidata response from making a query on a QID */
@Serializable data class WikidataResponse(val entities: Map<String, EntityInfo>)

@Serializable
data class EntityInfo(
  val labels: EnString,
  val descriptions: EnString,
  val claims: Map<String, List<WikiClaim>>,
  val sitelinks: SiteLinks,
)

@Serializable data class EnString(val en: StringValue? = null)

@Serializable data class StringValue(val value: String)

@Serializable data class SiteLinks(val enwiki: WikiLink)

@Serializable data class WikiLink(val url: String)

@Serializable data class WikiClaim(val mainsnak: MainSnak)

@Serializable
data class MainSnak(val snaktype: String, val datatype: String, val datavalue: DataValue? = null)

@Serializable data class DataValue(val value: JsonElement)

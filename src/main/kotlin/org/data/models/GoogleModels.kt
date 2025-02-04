package org.data.models

import kotlinx.serialization.Serializable

/** The classes below are used to extract the information made in Google Knowledge HTTP requests. */
@Serializable data class ItemListElement(val itemListElement: List<ResultObject>)

@Serializable data class ResultObject(val result: NameObject)

@Serializable data class NameObject(val name: String)

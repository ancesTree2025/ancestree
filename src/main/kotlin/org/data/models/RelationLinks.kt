package org.data.models

import kotlinx.serialization.Serializable

@Serializable data class RelationLinks(val relation: String, val links: List<String>)

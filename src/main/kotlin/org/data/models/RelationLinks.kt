package org.data.models

import kotlinx.serialization.Serializable
import org.domain.models.Graph

@Serializable data class RelationLinks(val relation: String, val links: Graph<Person>)

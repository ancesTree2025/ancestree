package org.domain.models

import kotlinx.serialization.Serializable

@Serializable data class AutocompleteResponse(val query: String, val autocomplete: List<String>)

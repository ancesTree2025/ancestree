package org.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalInfo(
  val image: String,
  val attributes: Map<String, String>,
  val description: String,
  val wikipedia_link: String,
)

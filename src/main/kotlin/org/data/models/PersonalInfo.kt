package org.data.models

import kotlinx.serialization.Serializable

/** Simple record structure to house personal information to be displayed on the side-bar. */
@Serializable
data class PersonalInfo(
  val image: String,
  val attributes: Map<String, String>,
  val description: String,
  val wikipedia_link: String,
)

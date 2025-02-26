package org.data.models

import kotlinx.serialization.Serializable

/** Data structure to specify information contained in the sidebar. Fields can be null. */
@Serializable
data class PersonalInfo(
  var image: String? = null,
  var birth: String? = null, // String of place and date of birth
  var death: String? = null, // String of place and date of death
  var description: String? = null,
  var wikiLink: String? = null,
)

package org.data.models

import kotlinx.serialization.Serializable

/** Simple record structure to house data for an individual's node. */
@Serializable
data class Person(
  val id: String,
  val name: String,
  val gender: String,
  val parents: List<String>,
  val spouses: List<String>,
  val children: List<String>,
)

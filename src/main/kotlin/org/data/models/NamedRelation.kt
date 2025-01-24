package org.data.models

data class NamedRelation(
  val Father: String,
  val Mother: String,
  val Gender: String,
  val Spouses: List<String>,
  val Children: List<String>,
  val Siblings: List<String>,
)

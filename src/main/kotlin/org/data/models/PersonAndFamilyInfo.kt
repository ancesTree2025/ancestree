package org.data.models

/** The data class returned by a query. Holds QID and name info, along with family data */
data class PersonAndFamilyInfo(
  val id: String,
  val name: String,
  val family: Map<String, List<String>>,
)

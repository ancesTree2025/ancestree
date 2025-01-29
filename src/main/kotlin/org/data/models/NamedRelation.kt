package org.data.models

/** Class to relations for a given entity */
class NamedRelation(
  val Father: String = "Unknown",
  val Mother: String = "Unknown",
  val Spouses: List<String> = emptyList(),
  val Children: List<String> = emptyList(),
  val Siblings: List<String> = emptyList(),
) {
  companion object {
    fun from(map: PropertyMapping): NamedRelation {
      return NamedRelation(
        Father = map["Father"]?.getOrNull(0) ?: "Unknown",
        Mother = map["Mother"]?.getOrNull(0) ?: "Unknown",
        Spouses = map["Spouse(s)"] ?: emptyList(),
        Children = map["Child(ren)"] ?: emptyList(),
        Siblings = map["Sibling(s)"] ?: emptyList(),
      )
    }
  }
}

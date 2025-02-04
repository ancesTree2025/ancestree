package org.data.models

object WikidataProperties {
  val propertyQIDMap =
    mapOf(
      "P21" to "Gender",
      "P22" to "Father",
      "P25" to "Mother",
      "P26" to "Spouse(s)",
      "P40" to "Child(ren)",
      "P3373" to "Sibling(s)",
    )

  val propertyQIDMapPersonal =
    mapOf(
      "P18" to "Wikimedia Image File",
      "P569" to "DoB",
      "P19" to "PoB",
      "P570" to "DoD",
      "P20" to "PoD",
    )
}

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
      "P1448" to "Brief Description", // This is wrong for now, will amend.
      "P18" to "Wikimedia Image File", // https://commons.wikimedia.org/wiki/Special:FilePath/{P18}
      "P569" to "DoB",
      "P19" to "PoB",
      "P570" to "DoD",
      "P20" to "PoD"
    )
}

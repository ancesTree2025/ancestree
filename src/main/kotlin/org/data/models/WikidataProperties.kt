package org.data.models

/** Object housing default property mappings used in query parsing. */
object WikidataProperties {
  // Family Relation Properties
  val propertyQIDMap =
    mapOf(
      "P21" to "Gender",
      "P22" to "Father",
      "P25" to "Mother",
      "P26" to "Spouse(s)",
      "P451" to "Partner(s)",
      "P40" to "Child(ren)",
    )

  // Personal Info Properties
  val propertyQIDMapPersonal =
    mapOf(
      "P18" to "Wikimedia Image File",
      "P569" to "DoB",
      "P19" to "PoB",
      "P570" to "DoD",
      "P20" to "PoD",
      "P625" to "Coords",
      "P551" to "Residence",
      "P39" to "Office Held",
      "P119" to "Burial",
    )

  // Destination Properties
  val propertyQIDMapDest = mapOf("P31" to "Instance Of")
}

package org.data.models

import kotlinx.serialization.Serializable

/** Simple record structure to house data for an individual's node.
 *  Default values denote an unknown person. */
@Serializable data class Person(val id: QID = "", val name: Label = "", val gender: String = "")

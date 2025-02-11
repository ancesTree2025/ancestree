package org.data.models

import kotlinx.serialization.Serializable

/** Simple record structure to house data for an individual's node. */
@Serializable data class Person(val id: QID = "", var name: Label = "", var gender: Label = "")

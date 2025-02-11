package org.data.models

import kotlinx.serialization.Serializable

/**
 * Simple record structure to house data for an individual's node. Default values denote an unknown
 * person.
 */
@Serializable data class Person(val qid: QID = "", var name: String = "", var gender: String = "")

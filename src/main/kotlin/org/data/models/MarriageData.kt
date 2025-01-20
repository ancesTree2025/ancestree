package org.data.models

/**
 * Simple record structure to house data for the marriage edge created between two people.
 */
data class MarriageData(val mother: String, val father: String, val children: List<String>)

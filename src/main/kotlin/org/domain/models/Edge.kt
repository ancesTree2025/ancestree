package org.domain.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Edge(
  val node1: String,
  val node2: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Edge) return false

    return (node1 == other.node1 && node2 == other.node2) ||
        (node1 == other.node2 && node2 == other.node1)
  }

  override fun hashCode(): Int {
    return Objects.hash(node1, node2)
  }
}
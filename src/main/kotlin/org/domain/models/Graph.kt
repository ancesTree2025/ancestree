package org.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Graph<T>(
  /* required for backend, can be ignored for frontend */
  /* only nullable when graph is empty */
  val root: Node<T>?,
  val nodes: Set<Node<T>>,
  val edges: Set<Edge>,
) {

  fun isEmpty(): Boolean {
    return root == null
  }
}

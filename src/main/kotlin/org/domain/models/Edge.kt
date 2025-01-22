package org.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Edge<T>(
  val node1: Node<T>,
  val node2: Node<T>,
  val children: List<Node<T>>
)
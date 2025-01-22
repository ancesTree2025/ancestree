package org.domain.models

data class Edge<T>(
  val node1: Node<T>,
  val node2: Node<T>,
  val children: List<Node<T>>
)
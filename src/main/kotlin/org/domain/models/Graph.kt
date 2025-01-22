package org.domain.models

import kotlinx.serialization.Serializable
import org.data.models.Person

@Serializable
data class Graph<T>(
  /* required for backend, can be ignored for frontend */
  /* only nullable when graph is empty */
  val root: Node<T>?,
  val nodes: Set<Node<T>>,
  val edges: Set<Edge<T>>
) {

  constructor(): this(null, emptySet(), emptySet())

  fun isEmpty(): Boolean {
    return root == null
  }
}

fun emptyGraph(): Graph<Person> {
  return Graph(null, emptySet(), emptySet())
}

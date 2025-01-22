package org.domain.models

import org.data.models.Person

data class Graph<T>(
  /* required for backend, can be ignored for frontend */
  /* only nullable when graph is empty */
  val root: Node<T>?,
  val nodes: Set<Node<T>>,
  val edges: Set<Edge<T>>
)

fun emptyGraph(): Graph<Person> {
  return Graph(null, emptySet(), emptySet())
}

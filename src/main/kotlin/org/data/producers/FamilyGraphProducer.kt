package org.data.producers

import org.data.models.Person
import org.data.services.LookupService
import org.domain.models.*
import org.domain.producers.GraphProducer
import kotlin.math.abs

/** A class to produce family nodes, which will be connected by marriage edges. */
class FamilyGraphProducer : GraphProducer<String, Person> {

  companion object {
    const val MAX_DEPTH = 2
  }

  override suspend fun produceGraph(root: String): Graph<Person> {
    val visited = setOf<Person>()
    return produceGraph(root, 0, visited)
  }

  private suspend fun produceGraph(query: String, depth: Int, visited: Set<Person>): Graph<Person> {
    if (abs(depth) > MAX_DEPTH) return emptyGraph()

    val rootNode = produceNode(query, depth)
    if (rootNode.data in visited) return emptyGraph()
  }


  /**
   * Produces a node for a particular person using cached results and Wiki queries.
   *
   * @param input An input string of a person's name.
   * @returns A node housing FamilyData, containing individual-specific information.
   */
  override suspend fun produceNode(query: String, depth: Int): Node<Person> {
    val tuple3 = LookupService.query(query)

    val qid = tuple3.first
    val label = tuple3.second.first
    val relation = tuple3.second.second

    val familyInfo = Person(qid, label, relation["Gender"]!![0])

    return Node(familyInfo, qid, depth)
  }
}

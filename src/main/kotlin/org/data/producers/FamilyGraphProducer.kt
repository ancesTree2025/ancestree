package org.data.producers

import kotlinx.coroutines.runBlocking
import org.core.prettyPrint
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

  private val visited = mutableSetOf<String>()

  override suspend fun produceGraph(root: String): Graph<Person> {
    visited.clear()
    return produceGraph(root, 0)
  }

  private suspend fun produceGraph(query: String, depth: Int): Graph<Person> {
    if (abs(depth) > MAX_DEPTH || query.isBlank()) return emptyGraph()

    val rootNode = produceNode(query, depth)
    if (rootNode.data.name in visited) return emptyGraph()
    visited.add(rootNode.data.name)

    val parentsGraph = rootNode.data.parents.map { produceGraph(it, depth - 1) }
    val spousesGraph = rootNode.data.spouses.map { produceGraph(it, depth) }
    val childrenGraph = rootNode.data.children.map { produceGraph(it, depth + 1) }

    val directEdges = setOf<Edge<Person>>()

    val nodes = (parentsGraph.flatMap(Graph<Person>::nodes)
        + spousesGraph.flatMap(Graph<Person>::nodes)
        + childrenGraph.flatMap(Graph<Person>::nodes)
        + rootNode).toSet()
    val edges = (parentsGraph.flatMap(Graph<Person>::edges)
        + spousesGraph.flatMap(Graph<Person>::edges)
        + childrenGraph.flatMap(Graph<Person>::edges)
        + directEdges).toSet()

    return Graph(
      root = rootNode,
      nodes = nodes,
      edges = edges
    )
  }


  /**
   * Produces a node for a particular person using cached results and Wiki queries.
   *
   * @param query An input string of a person's name.
   * @returns A node housing FamilyData, containing individual-specific information.
   */
  override suspend fun produceNode(query: String, depth: Int): Node<Person> {
    val tuple3 = LookupService.query(query)

    val qid = tuple3.first
    val label = tuple3.second.first
    val relation = tuple3.second.second

    val familyInfo = Person(
      id = qid,
      name = label,
      gender = relation["Gender"]?.getOrElse(0) {"prefer not to say"} ?: "", // FIXME please honestly kill me
      parents = listOf(relation["Father"]!!.getOrElse(0) { "" }, relation["Mother"]!!.getOrElse(0) { "" }), // fixme kill me again
      spouses = relation["Spouse(s)"]!!,
      children = relation["Child(ren)"]!!
    )

    return Node(familyInfo, qid, depth)
  }
}

fun main() {
  runBlocking {
    FamilyGraphProducer().produceGraph("Elon Musk").also { it.prettyPrint().also(::println) }
  }
}
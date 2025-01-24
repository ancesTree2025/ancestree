package org.data.producers

import io.ktor.util.reflect.*
import kotlin.math.abs
import org.data.models.Label
import org.data.models.Person
import org.data.services.WikiLookupService
import org.domain.models.*
import org.domain.producers.GraphProducer

/** A class to produce family nodes, which will be connected by marriage edges. */
class FamilyGraphProducer : GraphProducer<Label, Person> {

  private data class PersonAndRelatives<T>(
    val node: Node<T>,
    val parents: List<String>,
    val spouses: List<String>,
    val children: List<String>,
  )

  companion object {
    const val MAX_DEPTH = 1
  }

  private val visited = mutableSetOf<String>()

  override suspend fun produceGraph(root: String): Graph<Person> {
    visited.clear()
    return produceGraph(root, 0)
  }

  private suspend fun produceGraph(query: String, depth: Int): Graph<Person> {
    if (abs(depth) > MAX_DEPTH || query.isBlank()) return emptyGraph()

    val nodeAndRelatives = produceNode(query, depth)
    val rootNode = nodeAndRelatives.node
    if (rootNode.data.id in visited) return emptyGraph()
    visited.add(rootNode.id)

    val parentsGraph = nodeAndRelatives.parents.map { produceGraph(it, depth - 1) }
    val spousesGraph = nodeAndRelatives.spouses.map { produceGraph(it, depth) }
    val childrenGraph = nodeAndRelatives.children.map { produceGraph(it, depth + 1) }

    val directEdges =
      spousesGraph.mapNotNull { spouse -> spouse.root?.let { Edge(rootNode.id, it.id) } } +
        childrenGraph.mapNotNull { child -> child.root?.let { Edge(rootNode.id, it.id) } }

    val nodes =
      (parentsGraph.flatMap(Graph<Person>::nodes) +
          spousesGraph.flatMap(Graph<Person>::nodes) +
          childrenGraph.flatMap(Graph<Person>::nodes) +
          rootNode)
        .toSet()
    val edges =
      (parentsGraph.flatMap(Graph<Person>::edges) +
          spousesGraph.flatMap(Graph<Person>::edges) +
          childrenGraph.flatMap(Graph<Person>::edges) +
          directEdges)
        .toSet()

    return Graph(root = rootNode, nodes = nodes, edges = edges)
  }

  /**
   * Produces a node for a particular person using cached results and Wiki queries.
   *
   * @param query An input string of a person's name.
   * @returns A node housing FamilyData, containing individual-specific information.
   */
  private suspend fun produceNode(query: String, depth: Int): PersonAndRelatives<Person> {
    val personFamilyInfo =
      WikiLookupService().query(query)
        ?: return PersonAndRelatives(
          Node(Person(query, query, "???"), query, depth),
          emptyList(),
          emptyList(),
          emptyList(),
        )

    val qid = personFamilyInfo.id
    val label = personFamilyInfo.name
    val relation = personFamilyInfo.family

    val familyInfo = Person(id = qid, name = label, gender = relation.Gender)

    val node = Node(familyInfo, qid, depth)

    return PersonAndRelatives(
      node,
      parents = listOf(relation.Father),
      spouses = relation.Spouses,
      children = relation.Children,
    )
  }
}

package org.data.producers

import kotlin.math.abs
import org.data.models.Label
import org.data.models.Person
import org.data.services.WikiLookupService
import org.domain.models.*
import org.domain.producers.GraphProducer

/** A class to produce family nodes, which will be connected by marriage edges. */
class FamilyGraphProducer : GraphProducer<Label, Person> {

  companion object {
    const val MAX_DEPTH = 1
  }

  private val visited = mutableSetOf<Label>()
  private val nodes = mutableMapOf<Label, Node<Person>>()
  private val edges = mutableSetOf<Edge>()

  override suspend fun produceGraph(root: Label): Graph<Person> {
    visited.clear()
    nodes.clear()
    edges.clear()
    val rootNode = produceGraph(root, 0) ?: return emptyGraph()
    return Graph(rootNode, nodes.values.toSet(), edges)
  }

  // new: returns null if no new node to be made
  // new: use name in visited instead of qid for now
  private suspend fun produceGraph(query: Label, depth: Int): Node<Person>? {
    if (abs(depth) > MAX_DEPTH || query.isBlank()) return null
    if (query in visited) return nodes[query]

    // return empty node if not found from lookup
    val wikiResponse =
      WikiLookupService().query(query) ?: return Node(
        Person("Missing", query, "???")
        , query, depth)

    val person = wikiResponse.first
    val relation = wikiResponse.second

    val rootNode =
      Node(Person(id = person.id, name = person.name, gender = person.gender), person.id, depth)

    visited.add(query)
    nodes.put(query, rootNode)

    val parents = listOf(relation.Father, relation.Mother)
    val spouses = relation.Spouses
    val children = relation.Children

    // recurse over relations
    parents.map { produceGraph(it, depth - 1) }
    val spouseNodes = spouses.map { produceGraph(it, depth) }
    val childNodes = children.map { produceGraph(it, depth + 1) }

    // adds edges to set
    spouseNodes.forEach { if (it != null) edges.add(Edge(rootNode.id, it.id)) }
    childNodes.forEach { if (it != null) edges.add(Edge(rootNode.id, it.id)) }

    return rootNode
  }
}

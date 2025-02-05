package org.data.producers

import kotlin.math.abs
import org.data.services.WikiLookupService
import org.domain.models.Edge
import org.domain.models.Graph
import org.domain.models.Node
import org.domain.producers.GraphProducer
import org.data.models.Person
import org.data.models.Label
import org.data.models.NamedRelation

class FamilyGraphProducer : GraphProducer<Label, Person> {

  companion object {
    const val MAX_DEPTH = 2
  }

  private val visited = mutableSetOf<Label>()
  private val nodes = mutableMapOf<Label, Node<Person>>()
  private val edges = mutableSetOf<Edge>()

  /**
   * An internal queue item used for BFS processing.
   *
   * @param name The query string (person's name) to be looked up.
   * @param depth The current depth of this node relative to the root.
   * @param parentId The QID of the parent node (or source node) that references this person.
   */
  private data class QueueItem(var name: String, val depth: Int, val parentId: String?)

  override suspend fun produceGraph(root: Label): Graph<Person> {
    visited.clear()
    nodes.clear()
    edges.clear()
    

    if (root=="favicon.ico") {
      return Graph(Node(Person(), "", 0), emptySet(), emptySet())
    }

    val queue = mutableListOf(QueueItem(root, 0, parentId = null))

    val wikiService = WikiLookupService()

    var rootLabel = ""

    while (queue.isNotEmpty()) {

      val batchItems = queue.filter { it.name !in visited && it.name.isNotBlank() && abs(it.depth) <= MAX_DEPTH }
      if (batchItems.isEmpty()) {
        break
      }

      val namesToQuery = batchItems.map { it.name }.distinct()

      val queryResults: List<Pair<Person, NamedRelation>> = wikiService.queryAll(namesToQuery)

      val resultMap: Map<String, Pair<Person, NamedRelation>> =
        queryResults.associateBy { it.first.name }

      for (item in batchItems) {

        if (item.depth == 0 && item.parentId == null && queryResults.isNotEmpty()) {
          item.name = resultMap.keys.toList()[0]
          rootLabel = item.name
        }

        visited.add(item.name)

        if (resultMap.containsKey(item.name)) {

          val (person, relation) =resultMap[item.name]!!

          if (!nodes.containsKey(person.id)) {
            nodes[person.id] = Node(person, person.id, item.depth)
          }
          item.parentId?.let { parentId ->
            edges.add(Edge(parentId, person.id))
          }

          if (abs(item.depth - 1) <= MAX_DEPTH) {
            if (relation.Father.isNotBlank() && relation.Father !in visited) {
              queue.add(QueueItem(relation.Father, item.depth - 1, person.id))
            }
            if (relation.Mother.isNotBlank() && relation.Mother !in visited) {
              queue.add(QueueItem(relation.Mother, item.depth - 1, person.id))
            }
          }

          if (abs(item.depth) <= MAX_DEPTH) {
            for (spouse in relation.Spouses) {
              if (spouse.isNotBlank() && spouse !in visited) {
                queue.add(QueueItem(spouse, item.depth, person.id))
              }
            }
          }

          if (abs(item.depth + 1) <= MAX_DEPTH) {
            for (child in relation.Children) {
              if (child.isNotBlank() && child !in visited) {
                queue.add(QueueItem(child, item.depth + 1, person.id))
              }
            }
          }
        } else {
          val dummyPerson = Person("???", item.name, "???")
          if (!nodes.containsKey(item.name)) {
            nodes[item.name] = Node(dummyPerson, item.name, item.depth)
          }
          item.parentId?.let { parentId ->
            edges.add(Edge(parentId, item.name))
          }
        }
      }
      queue.removeAll { it.name in visited }
    }

    val rootNode = nodes.values.find { it.data.name == rootLabel }
      ?: error("Root not found...")
    return Graph(rootNode, nodes.values.toSet(), edges)
  }
}

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

  private val processedNodes = mutableSetOf<Label>()
  private val nodes = mutableMapOf<Label, Node<Person>>()
  private val edges = mutableSetOf<Edge>()

  private val childToParents = mutableMapOf<String, MutableSet<String>>()

  private enum class RelationType {
    Parent,
    Child,
    Spouse,
    Root
  }

  /**
   * An internal queue item used for BFS processing.
   *
   * @param name The query string (person's name) to be looked up.
   * @param depth The current depth of this node relative to the root.
   * @param parentId The QID of the parent node (or source node) that references this person.
   * @param type The type of relation that got us here.
   */
  private data class QueueItem(
    var name: String,
    val depth: Int,
    val parentId: String?,
    val type: RelationType
  )

  private fun addBidirectionalEdge(a: String, b: String) {
    edges.add(Edge(a, b))
    edges.add(Edge(b, a))
  }

  override suspend fun produceGraph(root: Label): Graph<Person> {
    processedNodes.clear()
    nodes.clear()
    edges.clear()
    childToParents.clear()

    if (root == "favicon.ico") {
      return Graph(Node(Person(), "", 0), emptySet(), emptySet())
    }

    var rootLabel = ""

    val queue = mutableListOf(QueueItem(root, 0, parentId = null, RelationType.Root))
    val wikiService = WikiLookupService()

    while (queue.isNotEmpty()) {
      val batchItems = queue.filter { it.name.isNotBlank() && abs(it.depth) <= MAX_DEPTH }
      if (batchItems.isEmpty()) break

      val namesToQuery = batchItems.map { it.name }.distinct()
      val queryResults: List<Pair<Person, NamedRelation>> = wikiService.queryAll(namesToQuery)
      val resultMap: Map<String, Pair<Person, NamedRelation>> =
        queryResults.associateBy { it.first.name }

      for (item in batchItems) {
        if (item.depth == 0 && item.parentId == null && queryResults.isNotEmpty()) {
          item.name = resultMap.keys.first()
          rootLabel = item.name
        }

        if (!resultMap.containsKey(item.name)) {
          continue
        }

        val (person, relation) = resultMap[item.name]!!

        if (!nodes.containsKey(person.id)) {
          nodes[person.id] = Node(person, person.id, item.depth)
        }

        when (item.type) {
          RelationType.Parent -> {
            edges.add(Edge(person.id, item.parentId!!))
            childToParents.getOrPut(item.parentId) { mutableSetOf() }.add(person.id)
          }
          RelationType.Child -> {
            edges.add(Edge(item.parentId!!, person.id))
            childToParents.getOrPut(person.id) { mutableSetOf() }.add(item.parentId)
          }
          RelationType.Spouse -> {
            addBidirectionalEdge(item.parentId!!, person.id)
          }
          RelationType.Root -> {
          }
        }

        if (!processedNodes.contains(item.name)) {
          if (abs(item.depth - 1) <= MAX_DEPTH) {
            if (relation.Father.isNotBlank()) {
              queue.add(QueueItem(relation.Father, item.depth - 1, person.id, RelationType.Parent))
            }
            if (relation.Mother.isNotBlank()) {
              queue.add(QueueItem(relation.Mother, item.depth - 1, person.id, RelationType.Parent))
            }
          }

          if (abs(item.depth) <= MAX_DEPTH) {
            for (spouse in relation.Spouses) {
              if (spouse.isNotBlank()) {
                queue.add(QueueItem(spouse, item.depth, person.id, RelationType.Spouse))
              }
            }
          }

          if (abs(item.depth + 1) <= MAX_DEPTH) {
            for (child in relation.Children) {
              if (child.isNotBlank()) {
                queue.add(QueueItem(child, item.depth + 1, person.id, RelationType.Child))
              }
            }
          }
          processedNodes.add(item.name)
        }
      }
      queue.removeAll { batchItems.contains(it) }
    }

    for ((_, parentSet) in childToParents) {
      val parents = parentSet.toList()
      if (parents.size >= 2) {
        for (i in parents.indices) {
          for (j in i + 1 until parents.size) {
            addBidirectionalEdge(parents[i], parents[j])
          }
        }
      }
    }

    val rootNode = nodes.values.find { it.data.name == rootLabel } ?: error("Root not found...")
    return Graph(rootNode, nodes.values.toSet(), edges)
  }
}

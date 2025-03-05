package org.data.producers

import kotlin.math.abs
import org.data.models.Label
import org.data.models.Person
import org.data.models.QID
import org.data.models.Relations
import org.data.services.WikiLookupService
import org.domain.models.Edge
import org.domain.models.Graph
import org.domain.models.Node
import org.domain.producers.GraphProducer

class FamilyGraphProducer : GraphProducer<Label, Person> {

  /** Various maps and sets to be used during graph generation. */
  private val visited = mutableSetOf<Pair<QID, QID>>()
  private val nodes = mutableMapOf<QID, Node<Person>>()
  private val edges = mutableSetOf<Edge>()
  private val genders = mutableSetOf<QID>()
  private val childToParents = mutableMapOf<String, MutableSet<String>>()

  /**
   * An enumeration which nodes will use to determine the relation between themselves and the person
   * represented by the node that enqueued their Q-item. So for example, if "Elon Musk" results in
   * "Justine Musk" being added to the queue, we would use Spouse.
   */
  private enum class RelationType {
    Parent,
    Child,
    Spouse,
    Root,
    Partner
  }

  /**
   * An internal queue item used for a BFS-style traversal.
   *
   * @param qid The QID of the person represented by the QueueItem.
   * @param depth The current depth of this node relative to the root.
   * @param parentId The QID of the node that references this person.
   * @param type See the description for RelationType.
   */
  private data class QueueItem(
    var qid: QID,
    val depth: Int,
    val parentId: String,
    val type: RelationType,
  )

  /**
   * It didn't really make sense to me to have edges from a spouse to one of their partners without
   * also having an edge for it the other way around - it's like a symmetric relation. This function
   * just does that.
   *
   * @param a One of the spouses.
   * @param b The other spouse.
   * @returns None.
   */
  private fun addSpousalEdges(a: String, b: String, married: Boolean = true) {
    var tag = ""
    if (married == false) {
      tag = "UN"
    }

    edges.add(Edge(a, b, tag))
    edges.add(Edge(b, a, tag))
  }

  /**
   * The new graph production algorithm, which uses a BFS traversal and leverages a batch request
   * method provided by the LookupService, for substantially faster times and fewer queries.
   *
   * @param root The initial string passed in by the user.
   * @returns The graph representation sent to the front-end.
   */
  override suspend fun produceGraph(root: Label, depth: Int, width: Int): Graph<Person> {

    /** Clearing the used sets */
    visited.clear()
    nodes.clear()
    edges.clear()
    childToParents.clear()
    genders.clear()

    /*TODO(Extremely bizarre bug, needs DESPERATE fixing.)
     * As of the latest push, I don't understand why this is happening, but after some debugging,
     * I found that for whatever reason, something is trying to produce a graph with this string.
     * Obviously it isn't a person and so on, so it just wastes time. For now, I just catch and discard it,
     * but we need to determine the underlying cause and remove it.
     * */
    if (root == "favicon.ico") {
      return Graph(Node(Person(), "", 0), emptySet(), emptySet())
    }

    /** Initializing service, queue and root */
    val wikiService = WikiLookupService()

    val rootInfo =
      wikiService.query(listOf(root)).getOrElse(0) { error("Root person failed query.") }
    val rootQid = rootInfo.first.id

    val queue = mutableListOf(QueueItem(rootQid, 0, parentId = "", RelationType.Root))

    var traversals = 0

    /** Beginning traversal */
    while (queue.isNotEmpty()) {

      /** Early stopping for exceeding horizontal width */
      println(traversals)
      if (traversals++ == width) {
        break
      }

      /** Producing the result map of QIDs to relations */
      val batchItems = queue.filter { abs(it.depth) <= depth }
      if (batchItems.isEmpty()) break

      val namesToQuery = batchItems.map { it.qid }.distinct()
      val queryResults: List<Pair<Person, Relations>> = wikiService.queryQIDS(namesToQuery)

      val resultMap: Map<String, Pair<Person, Relations>> = queryResults.associateBy { it.first.id }

      /** Iterating through batch items to repopulate queue and create edges */
      for (item in batchItems) {

        if (!resultMap.containsKey(item.qid)) {
          println("For some reason, ${item.qid} could not be processed.")
          continue
        }

        val (person, relation) = resultMap[item.qid]!!
        if (person.gender != "Unknown") {
          genders.add(person.gender)
        }
        if (!nodes.containsKey(person.id)) {
          nodes[person.id] = Node(person, person.id, item.depth)
        }

        /** Creating edges */
        when (item.type) {
          RelationType.Parent -> {
            edges.add(Edge(person.id, item.parentId))
            childToParents.getOrPut(item.parentId) { mutableSetOf() }.add(person.id)
          }
          RelationType.Child -> {
            edges.add(Edge(item.parentId, person.id))
            childToParents.getOrPut(person.id) { mutableSetOf() }.add(item.parentId)
          }
          RelationType.Spouse -> {
            addSpousalEdges(item.parentId, person.id)
          }
          RelationType.Partner -> {
            addSpousalEdges(item.parentId, person.id, false)
          }
          RelationType.Root -> {}
        }

        /** Repopulating queue */
        if (!visited.contains(Pair(item.qid, item.parentId))) {

          if (abs(item.depth - 1) <= depth) {
            if (relation.Father.isNotBlank()) {
              queue.add(QueueItem(relation.Father, item.depth - 1, person.id, RelationType.Parent))
            }
            if (relation.Mother.isNotBlank()) {
              queue.add(QueueItem(relation.Mother, item.depth - 1, person.id, RelationType.Parent))
            }
          }

          if (abs(item.depth) <= depth) {
            for (spouse in relation.Spouses) {
              if (spouse.isNotBlank()) {
                queue.add(QueueItem(spouse, item.depth, person.id, RelationType.Spouse))
              }
            }
          }

          if (abs(item.depth) <= depth) {
            for (spouse in relation.Partners) {
              if (spouse.isNotBlank()) {
                queue.add(QueueItem(spouse, item.depth, person.id, RelationType.Partner))
              }
            }
          }

          if (abs(item.depth + 1) <= depth) {
            for (child in relation.Children) {
              if (child.isNotBlank()) {
                queue.add(QueueItem(child, item.depth + 1, person.id, RelationType.Child))
              }
            }
          }
          visited.add(Pair(item.qid, item.parentId))
        }
      }

      queue.removeAll { batchItems.contains(it) }
    }

    /** Connecting unmarried people with children */
//    for ((_, parentSet) in childToParents) {
//      val parents = parentSet.toList()
//      if (parents.size >= 2) {
//        for (i in parents.indices) {
//          for (j in i + 1 until parents.size) {
//            addSpousalEdges(parents[i], parents[j], married = false)
//          }
//        }
//      }
//    }

    /** Re-labelling nodes with name and gender, from QID */
    val qidsToReplace = mutableListOf<QID>()
    qidsToReplace.addAll(nodes.keys)
    qidsToReplace.addAll(genders)

    println(qidsToReplace)
    println(qidsToReplace.size)

    val labels = wikiService.getAllLabels(qidsToReplace).toMutableMap()
    labels["Unknown"] = "Unknown"

    nodes.map {
      it.value.data.name = labels[it.key]!!
      it.value.data.gender = labels[it.value.data.gender]!!
    }

    val rootNode = nodes.values.find { it.data.id == rootQid } ?: error("Root not found...")

    val final = Graph(rootNode, nodes.values.toSet(), edges.toSet())

    print("Caching final graph: $final")

    return final
  }
}

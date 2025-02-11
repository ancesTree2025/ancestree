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

  /** A companion object housing graph configuration details. */
  companion object {
    const val MAX_DEPTH = 2
  }

  /** Various maps and sets to be used during graph generation. */
  private val visited = mutableSetOf<QID>()
  private val nodes = mutableMapOf<QID, Node<Person>>()
  private val edges = mutableSetOf<Edge>()
  private val genders = mutableSetOf<QID>()

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
  }

  /**
   * An internal queue item used for a BFS-style traversal.
   *
   * @param The query string (person's name) to be looked up. In the first iteration with the root
   *   string, this will be what the user types into ancestree, but after that we can expect a
   *   formal Wikidata label.
   * @param depth The current depth of this node relative to the root.
   * @param parentId The QID of the node that references this person.
   * @param type See the description for RelationType.
   *
   * TODO(Change this description! We use QID now!!!)
   */
  private data class QueueItem(
    var qid: QID,
    val depth: Int,
    val parentId: String?,
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
  private fun addSpousalEdges(a: String, b: String) {
    edges.add(Edge(a, b))
    edges.add(Edge(b, a))
  }

  /**
   * The new graph production algorithm, which uses a BFS traversal and leverages a batch request
   * method provided by the LookupService, for substantially faster times and fewer queries.
   *
   * @param root The initial string passed in by the user.
   * @returns The graph representation sent to the front-end.
   */
  override suspend fun produceGraph(root: Label): Graph<Person> {

    visited.clear()
    nodes.clear()
    edges.clear()

    if (root == "favicon.ico") {
      return Graph(Node(Person(), "", 0), emptySet(), emptySet())
    }

    /* We initialise the service we will be using, and a queue with just the root information in it. */
    val wikiService = WikiLookupService()

    val rootInfo =
      wikiService.query(listOf(root)).getOrElse(0) { error("Root person failed query.") }
    val rootQid = rootInfo.first.id

    val queue = mutableListOf(QueueItem(rootQid, 0, parentId = null, RelationType.Root))

    /* Now we start traversal... */
    while (queue.isNotEmpty()) {

      val batchItems = queue.filter { abs(it.depth) <= MAX_DEPTH }
      if (batchItems.isEmpty()) break

      val namesToQuery = batchItems.map { it.qid }.distinct()

      val queryResults: List<Pair<Person, Relations>> = wikiService.queryQIDS(namesToQuery)

      val resultMap: Map<String, Pair<Person, Relations>> = queryResults.associateBy { it.first.id }

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

        when (item.type) {
          RelationType.Parent -> {
            edges.add(Edge(person.id, item.parentId!!))
          }
          RelationType.Child -> {
            edges.add(Edge(item.parentId!!, person.id))
          }
          RelationType.Spouse -> {
            addSpousalEdges(item.parentId!!, person.id)
          }
          RelationType.Root -> {}
        }

        if (!visited.contains(item.qid)) {

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
          visited.add(item.qid)
        }
      }

      queue.removeAll { batchItems.contains(it) }
    }

    val qidsToReplace = mutableListOf<QID>()
    qidsToReplace.addAll(nodes.keys)
    qidsToReplace.addAll(genders)

    val labels = wikiService.getAllLabels(qidsToReplace).toMutableMap()
    labels["Unknown"] = "Unknown"

    nodes.map {
      it.value.data.name = labels[it.key]!!
      it.value.data.gender = labels[it.value.data.gender]!!
    }

    val rootNode = nodes.values.find { it.data.id == rootQid } ?: error("Root not found...")

    return Graph(rootNode, nodes.values.toSet(), edges)
  }
}

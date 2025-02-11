package org.data.producers

import kotlin.math.abs
import org.data.models.Label
import org.data.models.Relations
import org.data.models.Person
import org.data.models.QID
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
  }

  /**
   * An internal queue item used for a BFS-style traversal.
   *
   * @param name The query string (person's name) to be looked up. In the first iteration with the
   *   root string, this will be what the user types into ancestree, but after that we can expect a
   *   formal Wikidata label.
   * @param depth The current depth of this node relative to the root.
   * @param parentId The QID of the node that references this person.
   * @param type See the description for RelationType.
   */
  private data class QueueItem(
    var name: String,
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

    /* Clearing everything when producing a new graph. */
    visited.clear()
    nodes.clear()
    edges.clear()
    childToParents.clear()

    /* As of the latest push, I don't understand why this is happening, but after some debugging,
     * I found that for whatever reason, something is trying to produce a graph with this string.
     * Obviously it isn't a person and so on, so it just wastes time. For now, I just catch and discard it,
     * but we need to determine the underlying cause and remove it. */
    if (root == "favicon.ico") {
      return Graph(Node(Person(), "", 0), emptySet(), emptySet())
    }

    /* This is used later to identify the root node. */
    var rootLabel = ""

    /* We initialise the service we will be using, and a queue with just the root information in it. */
    val queue = mutableListOf(QueueItem(root, 0, parentId = null, RelationType.Root))
    val wikiService = WikiLookupService()

    /* Now we start traversal... */
    while (queue.isNotEmpty()) {

      /* Every time we do a step of traversal, we treat our new batch as everything in the queue which
       * isn't "" (an empty string). I initially also had this ignoring visited things, but we can't do that as
       * we need to revisit things for every way in which people are related to them (i.e., we need to visit
       * a child from both it's mother and it's father). There are probably ways to optimise this, but
       * none that I can be bothered to implement right now. */
      val batchItems = queue.filter { it.name.isNotBlank() && abs(it.depth) <= MAX_DEPTH }
      if (batchItems.isEmpty()) break

      /* After filtering, we batch-query all the unique names that appear in it. This is where we were
       * wasting most of our time with the initial algorithm. We then create resultsMap, which maps
       * a person's name (i.e., Hitler) to their Person object and relation mapping. */
      val namesToQuery = batchItems.map { it.name }.distinct()
      val queryResults: List<Pair<Person, Relations>> = wikiService.queryAll(namesToQuery)
      val resultMap: Map<String, Pair<Person, Relations>> =
        queryResults.associateBy { it.first.name }

      /* We now go over each of our items in the batch. */
      for (item in batchItems) {

        /* In the case of our root, we know that the string a user passes in may not necessarily be the
         * same as that persons official label on Wikipedia. Someone might type in "baracko bama" and we do
         * NOT want to use this, we want to use "Barack Obama". This is why we initialise the root label.
         * We also replace the batchItem string with this. */
        if (item.depth == 0 && item.parentId == null && queryResults.isNotEmpty()) {
          item.name = resultMap.keys.first()
          rootLabel = item.name
        }

        /* In the event that for whatever reason, a person in the batch didn't show up in the query, we
         * ignore and continue to the next batchItem. */
        if (!resultMap.containsKey(item.name)) {
          continue
        }

        /* We then extract the Person object and relation mapping, and create a new node if one doesn't
         * already exist. */
        val (person, relation) = resultMap[item.name]!!
        if (!nodes.containsKey(person.id)) {
          nodes[person.id] = Node(person, person.id, item.depth)
        }

        /* Depending on the type of the relation, we then create edges, and also add parents and children to
         * a childToParents map. This map basically gets used later to ensure we have edges between both the
         * parents of a particular child, as this is how we render trees in the front-end. */
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
            addSpousalEdges(item.parentId!!, person.id)
          }
          RelationType.Root -> {
            /* In this root case, we do nothing as there wasn't a preceding node that enqueued us. */
          }
        }

        /* If we haven't visited a node before, this means that we haven't enqueued the people it would
         * add to the graph, like the node's parents, spouses and kids. We just have code for each of these.  */
        if (!visited.contains(item.name)) {

          /* Parent case... */
          if (abs(item.depth - 1) <= MAX_DEPTH) {
            if (relation.Father.isNotBlank()) {
              queue.add(QueueItem(relation.Father, item.depth - 1, person.id, RelationType.Parent))
            }
            if (relation.Mother.isNotBlank()) {
              queue.add(QueueItem(relation.Mother, item.depth - 1, person.id, RelationType.Parent))
            }
          }

          /* Spouses case... */
          if (abs(item.depth) <= MAX_DEPTH) {
            for (spouse in relation.Spouses) {
              if (spouse.isNotBlank()) {
                queue.add(QueueItem(spouse, item.depth, person.id, RelationType.Spouse))
              }
            }
          }

          /* Kids case... */
          if (abs(item.depth + 1) <= MAX_DEPTH) {
            for (child in relation.Children) {
              if (child.isNotBlank()) {
                queue.add(QueueItem(child, item.depth + 1, person.id, RelationType.Child))
              }
            }
          }
          visited.add(item.name)
        }

        /* After this, we go onto our next iteration of batching and traversing. */
      }

      /* When we reach this point, we have processed everything that was part of the current batch.
       * We therefore just remove them from the overall queue, and move on.  */
      queue.removeAll { batchItems.contains(it) }
    }

    /* Simple code which adds edges between spouses who share children. This pretty much just
     * ensures they exist even if not explicitly added by the graph generation above. */
    for ((_, parentSet) in childToParents) {
      val parents = parentSet.toList()
      if (parents.size >= 2) {
        for (i in parents.indices) {
          for (j in i + 1 until parents.size) {
            addSpousalEdges(parents[i], parents[j])
          }
        }
      }
    }

    /* Lastly, we re-find the root node using the rootLabel we defined earlier, and then we return
     * the graph. */
    val rootNode = nodes.values.find { it.data.name == rootLabel } ?: error("Root not found...")
    return Graph(rootNode, nodes.values.toSet(), edges)
  }
}

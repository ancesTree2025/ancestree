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

    println("Starting graph production for root: $root")

    if (root=="favicon.ico") {
//      println("Throwing favicon.ico (I'm not sure why we get this)...")
      return Graph(Node(Person(), "", 0), emptySet(), emptySet())
    }

    val queue = mutableListOf(QueueItem(root, 0, parentId = null))
//    println("Initial queue: $queue")

    val wikiService = WikiLookupService()

    var rootLabel = ""

    while (queue.isNotEmpty()) {
//      println("----- New BFS Iteration -----")

      val batchItems = queue.filter { it.name !in visited && it.name.isNotBlank() && abs(it.depth) <= MAX_DEPTH }
//      println("Filtered queue items: $batchItems")
      if (batchItems.isEmpty()) {
//        println(" - Breaking due to empty batch items.")
        break
      }

      val namesToQuery = batchItems.map { it.name }.distinct()
//      println("Names to query: namesToQuery")

      val queryResults: List<Pair<Person, NamedRelation>> = wikiService.queryAll(namesToQuery)
//      println("Result of query: queryResults")


      val resultMap: Map<String, Pair<Person, NamedRelation>> =
        queryResults.associateBy { it.first.name }
//      println("Mapping of Labels to Pairs:\n$resultMap")

      for (item in batchItems) {
//        println(" - Processing queue item: $item")

        if (item.depth == 0 && item.parentId == null && queryResults.isNotEmpty()) {
//          println(" - Is root node, so replacing input with formal label...")
          item.name = resultMap.keys.toList()[0]
          rootLabel = item.name
        }

        visited.add(item.name)
//        println("   -> Added '${item.name}' to visited set.")

        if (resultMap.containsKey(item.name)) {

          val (person, relation) =resultMap[item.name]!!

//          println("   -> Lookup found for '${item.name}': Person: $person, Relation: $relation")
          if (!nodes.containsKey(person.id)) {
            nodes[person.id] = Node(person, person.id, item.depth)
//            println("   -> Added node for person with id '${person.id}' at depth ${item.depth}")
          }
          item.parentId?.let { parentId ->
            edges.add(Edge(parentId, person.id))
//            println("   -> Added edge from parent '$parentId' to '${person.id}'")
          }

          if (abs(item.depth - 1) <= MAX_DEPTH) {
            if (relation.Father.isNotBlank() && relation.Father !in visited) {
              queue.add(QueueItem(relation.Father, item.depth - 1, person.id))
//              println("   -> Enqueued Father '${relation.Father}' with depth ${item.depth - 1} from '${person.id}'")
            }
            if (relation.Mother.isNotBlank() && relation.Mother !in visited) {
              queue.add(QueueItem(relation.Mother, item.depth - 1, person.id))
//              println("   -> Enqueued Mother '${relation.Mother}' with depth ${item.depth - 1} from '${person.id}'")
            }
          }

          if (abs(item.depth) <= MAX_DEPTH) {
            for (spouse in relation.Spouses) {
              if (spouse.isNotBlank() && spouse !in visited) {
                queue.add(QueueItem(spouse, item.depth, person.id))
//                println("   -> Enqueued Spouse '$spouse' with depth ${item.depth} from '${person.id}'")
              }
            }
          }

          if (abs(item.depth + 1) <= MAX_DEPTH) {
            for (child in relation.Children) {
              if (child.isNotBlank() && child !in visited) {
                queue.add(QueueItem(child, item.depth + 1, person.id))
//                println("   -> Enqueued Child '$child' with depth ${item.depth + 1} from '${person.id}'")
              }
            }
          }
        } else {
//          println("   -> No lookup result found for '${item.name}'. Creating dummy node.")
          val dummyPerson = Person("???", item.name, "???")
          if (!nodes.containsKey(item.name)) {
            nodes[item.name] = Node(dummyPerson, item.name, item.depth)
//            println("   -> Added dummy node for '${item.name}' at depth ${item.depth}")
          }
          item.parentId?.let { parentId ->
            edges.add(Edge(parentId, item.name))
//            println("   -> Added edge from parent '$parentId' to dummy node '${item.name}'")
          }
        }
      }
      queue.removeAll { it.name in visited }
//      println("Queue after removal of visited items: $queue")
//      println("Nodes so far: $nodes")
//      println("Edges so far: $edges")
    }

//    println("Finished processing all queue items.")
    val rootNode = nodes.values.find { it.data.name == rootLabel }
      ?: error("Root not found...")
//    println("Root node determined as: $rootNode")
    return Graph(rootNode, nodes.values.toSet(), edges)
  }
}

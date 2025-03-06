package org.data.services

import org.data.caches.WikiCacheManager
import org.data.models.*
import org.data.parsers.WikiRequestParser
import org.data.requests.ComplexRequester
import org.domain.models.*

class GraphTrawlService {

  private val contractionRules: Map<List<String>, String> =
    mapOf(
      // Sibling relations from parent/child pairs.
      listOf("Father", "Son") to "Brother",
      listOf("Mother", "Daughter") to "Sister",
      listOf("Father", "Daughter") to "Sister",
      listOf("Mother", "Son") to "Brother",

      // Parent sibling relations.
      listOf("Mother", "Brother") to "Uncle",
      listOf("Father", "Sister") to "Aunt",
      listOf("Father", "Brother") to "Uncle",
      listOf("Mother", "Sister") to "Aunt",

      // Child sibling relations.
      listOf("Brother", "Son") to "Nephew",
      listOf("Sister", "Daughter") to "Niece",
      listOf("Sister", "Son") to "Nephew",
      listOf("Brother", "Daughter") to "Niece",

      // Cousin relationships.
      listOf("Uncle", "Son") to "Cousin",
      listOf("Uncle", "Daughter") to "Cousin",
      listOf("Aunt", "Son") to "Cousin",
      listOf("Aunt", "Daughter") to "Cousin",

      // In-law relationships.
      listOf("Spouse", "Mother") to "Mother-in-law",
      listOf("Spouse", "Father") to "Father-in-law",
      listOf("Spouse", "Brother") to "Brother-in-law",
      listOf("Spouse", "Sister") to "Sister-in-law",

      // If the in-law token appears second:
      listOf("Brother", "Spouse") to "Sister-in-law",
      listOf("Sister", "Spouse") to "Brother-in-law",
    )

  fun deltaForRelation(rel: String): Int {
    return when {
      rel.startsWith("Father") || rel.startsWith("Mother") -> -1
      rel.startsWith("Son") || rel.startsWith("Daughter") -> 1
      rel.startsWith("Wife") || rel.startsWith("Husband") -> 0
      else -> 0
    }
  }

  private fun applyDictionaryContractions(inputTokens: List<String>): List<String> {
    val result = mutableListOf<String>()
    var i = 0
    while (i < inputTokens.size) {
      if (i < inputTokens.size - 1) {
        val pair = listOf(inputTokens[i], inputTokens[i + 1])
        if (contractionRules.containsKey(pair)) {
          result.add(contractionRules[pair]!!)
          i += 2
          continue
        }
      }
      result.add(inputTokens[i])
      i++
    }
    return result
  }

  private fun applyRunContractions(inputTokens: List<String>): List<String> {
    val result = mutableListOf<String>()
    var i = 0
    while (i < inputTokens.size) {
      val token = inputTokens[i]
      if (token.equals("Mother", ignoreCase = true) || token.equals("Father", ignoreCase = true)) {
        val type = token.replaceFirstChar { it.uppercaseChar() }
        var count = 1
        while (
          i + count < inputTokens.size &&
            (inputTokens[i + count].equals("mother", ignoreCase = true) ||
              inputTokens[i + count].equals("father", ignoreCase = true))
        ) {
          count++
        }
        when (count) {
          1 -> result.add(type)
          2 -> result.add("Grand$type")
          else -> {
            val greats = "Great-".repeat(count - 2)
            result.add(greats + "Grand" + type)
          }
        }
        i += count
      } else if (
        token.equals("Son", ignoreCase = true) || token.equals("Daughter", ignoreCase = true)
      ) {
        val type = token.replaceFirstChar { it.uppercaseChar() }
        var count = 1
        while (
          i + count < inputTokens.size &&
            (inputTokens[i + count].equals("son", ignoreCase = true) ||
              inputTokens[i + count].equals("daughter", ignoreCase = true))
        ) {
          count++
        }
        when (count) {
          1 -> result.add(type)
          2 -> result.add("Grand$type")
          else -> {
            val greats = "Great-".repeat(count - 2)
            result.add(greats + "Grand" + type)
          }
        }
        i += count
      } else {
        result.add(token)
        i++
      }
    }
    return result
  }

  private fun contractFullRelation(initialRelation: String): String {
    var tokens = initialRelation.split("'s").map { it.trim() }.filter { it.isNotEmpty() }

    while (true) {
      val afterDict = applyDictionaryContractions(tokens)
      val afterRun = applyRunContractions(afterDict)
      if (afterRun == tokens) break
      tokens = afterRun
    }

    return tokens.joinToString("'s ")
  }

  data class Path(val qids: List<QID>, val rels: List<String>)

  private suspend fun ensurePropsFor(qids: Collection<QID>) {

    val missing = qids.filter { WikiCacheManager.getProps(it) == null }.distinct()

    if (missing.isNotEmpty()) {

      val subqs = missing.chunked(45)
      subqs.forEach {
        val propReq = ComplexRequester.getInfo(it)
        val qidToPropertyMap = WikiRequestParser.parseWikidataClaims(propReq)
        missing.forEach { qid ->
          WikiCacheManager.putProps(qid, qidToPropertyMap[qid] ?: emptyMap())
        }
      }
    }
  }

  suspend fun getRelation(origin: QID, destLab: Label): RelationLinks {

    val dest: QID? =
      WikiCacheManager.getQID(destLab)
        ?: run {
          val qidReq = ComplexRequester.searchWikidataForQID(destLab)
          WikiRequestParser.parseWikidataIDLookup(qidReq)
        }

    if (dest == null)
      return RelationLinks("Unrelated", Graph(Node(Person(), "", 0), emptySet(), emptySet()))

    val visited = mutableSetOf(origin)
    var currentLevelQueue = mutableListOf(Path(listOf(origin), emptyList()))
    val maxDepth = 6

    while (currentLevelQueue.isNotEmpty()) {

      val currentNodes = currentLevelQueue.map { it.qids.last() }.toSet()
      ensurePropsFor(currentNodes)

      val missingGenderNeighbors = mutableSetOf<QID>()
      for (path in currentLevelQueue) {
        val currentQID = path.qids.last()
        val propMap = WikiCacheManager.getProps(currentQID) ?: emptyMap()
        propMap.forEach { (prop, qidList) ->
          if (prop in listOf("Spouse(s)", "Child(ren)")) {
            qidList.forEach { neighbor ->
              if (WikiCacheManager.getProps(neighbor)?.get("Gender") == null) {
                missingGenderNeighbors.add(neighbor)
              }
            }
          }
        }
      }

      if (missingGenderNeighbors.isNotEmpty()) {
        ensurePropsFor(missingGenderNeighbors)
      }

      val nextLevelQueue = mutableListOf<Path>()

      for (path in currentLevelQueue) {
        if (path.qids.size > maxDepth) continue

        val currentQID = path.qids.last()
        val propMap = WikiCacheManager.getProps(currentQID) ?: emptyMap()

        propMap.forEach { (prop, qidList) ->
          if (prop == "Gender") return@forEach

          qidList.forEach { neighbor ->
            val relationLabel =
              when (prop) {
                "Father" -> "Father's"
                "Mother" -> "Mother's"
                "Spouse(s)" -> {
                  val gender =
                    WikiCacheManager.getProps(neighbor)?.get("Gender")?.firstOrNull() ?: "unknown"
                  if (gender.equals("Q6581072", ignoreCase = true)) {
                    "Wife's"
                  } else if (gender.equals("Q6581097", ignoreCase = true)) {
                    "Husband's"
                  } else {
                    "Spouse's"
                  }
                }
                "Child(ren)" -> {
                  val gender =
                    WikiCacheManager.getProps(neighbor)?.get("Gender")?.firstOrNull() ?: "unknown"
                  if (gender.equals("Q6581072", ignoreCase = true)) {
                    "Daughter's"
                  } else if (gender.equals("Q6581097", ignoreCase = true)) {
                    "Son's"
                  } else {
                    "Child's"
                  }
                }
                else -> null
              } ?: return@forEach

            if (neighbor !in visited) {
              visited.add(neighbor)
              val newPath = Path(path.qids + neighbor, path.rels + relationLabel)

              if (neighbor == dest) {
                val chain = newPath.qids
                val rawRels = newPath.rels

                val depths = mutableListOf<Int>()
                var currentDepth = 0
                depths.add(currentDepth)
                rawRels.forEach { rel ->
                  currentDepth += deltaForRelation(rel)
                  depths.add(currentDepth)
                }

                val service = WikiLookupService()
                val queryResults: List<Pair<Person, Relations>> = service.queryQIDS(chain)
                val personMap: Map<QID, Person> = queryResults.associate { it.first.id to it.first }

                val nodes = mutableMapOf<QID, Node<Person>>()
                chain.forEachIndexed { idx, qid ->
                  val person = personMap[qid] ?: Person()
                  nodes[qid] = Node(person, qid, depths.getOrElse(idx) { 0 })
                }

                val edges = mutableSetOf<Edge>()
                for (i in 0 until chain.size - 1) {
                  edges.add(Edge(chain[i], chain[i + 1]))
                }

                for (i in 0 until rawRels.size) {
                  val rel = rawRels[i]
                  if (
                    rel.startsWith("Father") ||
                      rel.startsWith("Mother") ||
                      rel.startsWith("Son") ||
                      rel.startsWith("Daughter")
                  ) {
                    val nodeA = nodes[chain[i]]!!
                    val nodeB = nodes[chain[i + 1]]!!
                    val (parentNode, childNode) =
                      if (nodeA.depth < nodeB.depth) Pair(nodeA, nodeB) else Pair(nodeB, nodeA)

                    val childProps = WikiCacheManager.getProps(childNode.id) ?: emptyMap()
                    val parentQIDs = mutableSetOf<String>()

                    listOf("Father", "Mother").forEach { parentType ->
                      val parentQID = childProps[parentType]?.firstOrNull()
                      if (parentQID != null && parentQID != parentNode.id) {
                        val missingQuery = service.queryQIDS(listOf(parentQID))
                        val missingParentPerson =
                          if (missingQuery.isNotEmpty()) missingQuery.first().first else Person()
                        val newNode = Node(missingParentPerson, parentQID, parentNode.depth)
                        nodes[parentQID] = newNode
                        edges.add(Edge(parentNode.id, parentQID))
                        edges.add(Edge(parentQID, parentNode.id))
                        edges.add(Edge(parentQID, childNode.id))
                        edges.add(Edge(childNode.id, parentQID))
                      }
                      if (parentQID != null) {
                        parentQIDs.add(parentQID)
                      }
                    }

                    if (parentQIDs.size > 1) {
                      val parentList = parentQIDs.toList()
                      for (j in 0 until parentList.size) {
                        for (k in j + 1 until parentList.size) {
                          edges.add(Edge(parentList[j], parentList[k]))
                          edges.add(Edge(parentList[k], parentList[j]))
                        }
                      }
                    }
                  }
                }

                val rootNode = nodes[chain.first()]!!

                val qidsToReplace = mutableSetOf<QID>()
                nodes.forEach { (qid, node) ->
                  qidsToReplace.add(qid)
                  qidsToReplace.add(node.data.gender)
                }
                val labels = service.getAllLabels(qidsToReplace.toList()).toMutableMap()
                labels["Unknown"] = "Unknown"
                nodes.forEach { (_, node) ->
                  node.data.name = labels[node.data.id] ?: node.data.name
                  node.data.gender = labels[node.data.gender] ?: node.data.gender
                }

                val rawRelString =
                  rawRels.joinToString(" ").let { if (it.endsWith("'s")) it.dropLast(2) else it }
                val relString = contractFullRelation(rawRelString)

                return RelationLinks(relString, Graph(rootNode, nodes.values.toSet(), edges))
              }

              nextLevelQueue.add(newPath)
            }
          }
        }
      }
      currentLevelQueue = nextLevelQueue
    }

    return RelationLinks("Unrelated", Graph(Node(Person(), "", 0), emptySet(), emptySet()))
  }
}

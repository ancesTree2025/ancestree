package org.data.services

import org.data.caches.WikiCacheManager
import org.data.models.*
import org.data.parsers.WikiRequestParser
import org.data.requests.ComplexRequester

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
          i + count < inputTokens.size && inputTokens[i + count].equals(token, ignoreCase = true)
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
      val propReq = ComplexRequester.getInfo(missing.toList())
      val qidToPropertyMap = WikiRequestParser.parseWikidataClaims(propReq)
      missing.forEach { qid -> WikiCacheManager.putProps(qid, qidToPropertyMap[qid] ?: emptyMap()) }
    }
  }

  suspend fun getRelation(origin: QID, destLab: Label): RelationLinks {

    val dest: QID? =
      WikiCacheManager.getQID(destLab)
        ?: run {
          val qidReq = ComplexRequester.searchWikidataForQID(destLab)
          WikiRequestParser.parseWikidataIDLookup(qidReq)
        }

    if (dest == null) return RelationLinks("Unrelated", listOf())
    if (origin == dest) return RelationLinks("Self", listOf())

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
                  if (gender.equals("female", ignoreCase = true)) "Wife's" else "Husband's"
                }
                "Child(ren)" -> {
                  val gender =
                    WikiCacheManager.getProps(neighbor)?.get("Gender")?.firstOrNull() ?: "unknown"
                  if (gender.equals("female", ignoreCase = true)) "Daughter's" else "Son's"
                }
                else -> null
              } ?: return@forEach

            if (neighbor !in visited) {
              visited.add(neighbor)

              val newPath = Path(path.qids + neighbor, path.rels + relationLabel)

              if (neighbor == dest) {
                val relString =
                  newPath.rels.joinToString(" ").let {
                    if (it.endsWith("'s")) it.dropLast(2) else it
                  }
                return RelationLinks(contractFullRelation(relString), newPath.qids.drop(1))
              }

              nextLevelQueue.add(newPath)
            }
          }
        }
      }
      currentLevelQueue = nextLevelQueue
    }

    return RelationLinks("Unrelated", listOf())
  }
}

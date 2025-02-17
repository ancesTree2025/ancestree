package org.data.services

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.data.caches.InMemWikiCacheManager
import org.data.caches.WikiCacheManager
import org.data.models.*
import org.data.models.WikidataProperties.propertyQIDMapPersonal
import org.data.parsers.GoogleKnowledgeRequestParser
import org.data.parsers.WikiRequestParser
import org.data.requests.ComplexRequester

/** Service class for performing Wikipedia/Wikidata lookups. */
class WikiLookupService : LookupService<String, Pair<Person, Relations>> {

  /** A companion object housing API configuration details. */
  companion object {
    const val CHUNK_SIZE = 45
    val cacheManager: WikiCacheManager = InMemWikiCacheManager
  }

  /**
   * Queries a list of names and returns their person objects and relations to family members.
   *
   * @param input A list of names.
   * @returns A list of person-relation pairs.
   */
  override suspend fun query(input: List<Label>): List<Pair<Person, Relations>> {

    val qids = mutableListOf<QID>()

    input.forEach {
      val qid = cacheManager.getQID(it)

      if (qid == null) {
        println("QID not found: $it")
      } else {
        qids.add(qid)
      }
    }

    return queryQIDS(qids)
  }

  /**
   * Queries a list of QIDs and returns their person objects and relations to family members.
   *
   * @param qids A list of QIDs.
   * @returns A list of person-relation pairs.
   */
  suspend fun queryQIDS(qids: List<QID>): List<Pair<Person, Relations>> {

    val unseenQids = mutableListOf<QID>()
    val finalRelations = mutableListOf<Pair<Person, Relations>>()

    // Get data for all QIDs in cache
    qids.forEach {
      val props = cacheManager.getProps(it)
      if (props == null) {
        unseenQids.add(it)
      } else {
        val person = Person(it, "", props["Gender"]?.getOrNull(0) ?: "Unknown")
        finalRelations.add(Pair(person, Relations.from(props)))
      }
    }

    if (unseenQids.isEmpty()) {
      return finalRelations
    }

    // Batch query for all new QIDs
    val mappings = mutableMapOf<QID, PropertyMapping>()
    val subQs = unseenQids.chunked(CHUNK_SIZE)
    subQs.forEach {
      val claimsResp = ComplexRequester.getLabelsOrClaims(it)
      mappings.putAll(WikiRequestParser.parseWikidataClaims(claimsResp))
    }

    unseenQids.forEach {
      val props = mappings[it]!!
      cacheManager.putProps(it, props)
      val person = Person(it, "", props["Gender"]?.getOrNull(0) ?: "Unknown")
      finalRelations.add(Pair(person, Relations.from(props)))
    }

    return finalRelations
  }

  /**
   * Makes queries to get detailed info on a person.
   *
   * @param qid The person's Wikidata QID.
   * @returns Various info and personal attributes.
   */
  suspend fun getDetailedInfo(qid: QID): PersonalInfo {

    val familyResp = ComplexRequester.getLabelsOrClaims(listOf(qid))
    val infoMap = WikiRequestParser.parseWikidataClaims(familyResp, propertyQIDMapPersonal)[qid]!!

    val imageString = mkImage(infoMap["Wikimedia Image File"]!!)

    val birthPlace = getPlaceName(infoMap["PoB"]!!.getOrNull(0))
    val deathPlace = getPlaceName(infoMap["PoD"]!!.getOrNull(0))

    // TODO: is this needed?
//    if (cacheManager.getLabel(qid) == null) {
//      val labelResp = ComplexRequester.getLabelsOrClaims(listOf(qid))
//      label = WikiRequestParser.parseWikidataLabels(labelResp)[qid]!!
//      cacheManager.putLabel(qid, label)
//    } else {
//      label = cacheManager.getLabel(qid)!!
//    }

    val info =
      PersonalInfo(
        imageString,
        mapOf(
          "Born" to formatDatePlaceInfo(birthPlace, infoMap["DoB"]),
          "Died" to formatDatePlaceInfo(deathPlace, infoMap["DoD"]),
        ),
        "description stub", // ChatGPTDescriptionService.summarise(allInfo[qid]!!.first),
        "wikipedia link stub",
      )

    return info
  }

  /**
   * Finds labels corresponding to the list of QIDs
   *
   * @param qids A list of QIDs.
   * @returns A map of QIDs to their respective labels.
   */
  suspend fun getAllLabels(qids: List<QID>): Map<QID, Label> {

    val unseenQids = mutableListOf<Label>()
    val finalLabels = mutableMapOf<QID, Label>()

    qids.forEach {
      if (cacheManager.getLabel(it) == null) {
        unseenQids.add(it)
      } else {
        finalLabels[it] = cacheManager.getLabel(it)!!
      }
    }

    if (unseenQids.isEmpty()) {
      return finalLabels
    }

    var c = 0
    val subQs = unseenQids.chunked(45)
    subQs.forEach {
      println("--${c++} out of ${(unseenQids.size/45)}$")
      val claimsResp = ComplexRequester.getLabelsOrClaims(it)
      val qidToLabels = WikiRequestParser.parseWikidataLabels(claimsResp)

      qidToLabels.forEach { (qid, label) -> cacheManager.putLabel(qid, label) }
      finalLabels.putAll(qidToLabels)
    }

    return finalLabels
  }

  /**
   * A simple function to format a date and a place into a single string for returning.
   *
   * @param place The place of birth/death.
   * @param date The time of birth/death.
   * @returns A various info and personal attributes.
   */
  private fun formatDatePlaceInfo(place: String, date: List<String>?): String {
    if (date.isNullOrEmpty() && place == "Unknown") {
      return "Unknown"
    }

    val dateString = date!![0].substringBefore("T").removePrefix("+")

    val fmtDate: String =
      try {
        LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("d/M/yyyy"))
      } catch (e: Throwable) {
        dateString.takeWhile { (it != '-') }
      }

    return "$place, $fmtDate."
  }

  /**
   * A simple function to query Wikidata to retrieve a place name using its QID.
   *
   * @param locQID The relevant place's QID.
   * @returns A various info and personal attributes.
   */
  private suspend fun getPlaceName(locQID: QID?): Label {
    if (locQID == null) return "Unknown"

    if (cacheManager.getLabel(locQID) == null) {
      val locReq = ComplexRequester.getLabelsOrClaims(listOf(locQID))
      val locInfo = WikiRequestParser.parseWikidataLabels(locReq)
      val name = locInfo[locQID]!!
      cacheManager.putLabel(locQID, name)
      return name
    } else {
      return cacheManager.getLabel(locQID)!!
    }
  }

  /**
   * A simple function to format an image URL in the style of Wikimedia Commons.
   *
   * @param images A list of relevant images, though we only consider index 0.
   * @returns A formed URL to query for the image.
   */
  private suspend fun mkImage(images: List<String>): String {
    if (images.isEmpty()) {
      return "https://upload.wikimedia.org/wikipedia/commons/a/ac/Default_pfp.jpg"
    }

    val formattedFilename = images[0].replace(" ", "_")

    val encodedFilename =
      withContext(Dispatchers.IO) {
          URLEncoder.encode(formattedFilename, StandardCharsets.UTF_8.toString())
        }
        .replace("+", "%20")

    return "https://commons.wikimedia.org/wiki/Special:FilePath/$encodedFilename"
  }

  /**
   * Fetches autocomplete results from the connected database for search input.
   *
   * @param input The part of the query that the user intends to ask.
   * @return A list of complete queries that the user may want to input.
   */
  suspend fun fetchAutocomplete(input: String): List<String> {
    val response = ComplexRequester.getAutocompleteNames(input)

    return GoogleKnowledgeRequestParser.parseGoogleKnowledgeLookup(response)
  }
}

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
    val cache_manager: WikiCacheManager = InMemWikiCacheManager
  }

  /**
   * Query function that takes in a number of names and returns pairs of their person objects and
   * relations for family members.
   *
   * @param input A list of names.
   * @returns A list of person-relation pairs.
   */
  override suspend fun query(input: List<Label>): List<Pair<Person, Relations>> {

    val qids = mutableListOf<QID>()

    input.forEach {
      if (cache_manager.getQID(it).isNullOrEmpty()) {

        val qidResp = ComplexRequester.searchWikidataForQID(it)
        val qid = WikiRequestParser.parseWikidataIDLookup(qidResp)

        if (qid == null) {
          println("QID not found: $it")
        } else {
          cache_manager.putQID(it, qid)
          qids.add(qid)
        }
      } else {
        qids.add(cache_manager.getQID(it)!!)
      }
    }

    return queryQIDS(qids)
  }

  /**
   * A QID-specific query function which fulfils much the same purpose of query.
   *
   * @param qids A list of QIDs.
   * @returns A list of person-relation pairs.
   */
  suspend fun queryQIDS(qids: List<QID>): List<Pair<Person, Relations>> {

    if (qids.isEmpty()) {
      return listOf()
    }

    val unseenQids = mutableListOf<QID>()
    val finalRelations = mutableListOf<Pair<Person, Relations>>()

    qids.forEach {
      if (cache_manager.getProps(it) == null) {
        unseenQids.add(it)
      } else {
        val props = cache_manager.getProps(it)!!
        val person = Person(it, "", props["Gender"]?.getOrNull(0) ?: "Unknown")
        finalRelations.add(Pair(person, Relations.from(props)))
      }
    }

    if (unseenQids.isEmpty()) {
      return finalRelations
    }

    val mappings = mutableMapOf<QID, PropertyMapping>()
    val subQs = unseenQids.chunked(CHUNK_SIZE)
    subQs.forEach {
      val claimsResp = ComplexRequester.getLabelsOrClaims(it)
      mappings.putAll(WikiRequestParser.parseWikidataClaims(claimsResp))
    }

    unseenQids.forEach {
      val props = mappings[it]!!
      cache_manager.putProps(it, props)
      val person = Person(it, "", props["Gender"]?.getOrNull(0) ?: "Unknown")
      finalRelations.add(Pair(person, Relations.from(props)))
    }

    return finalRelations
  }

  /**
   * A new exposed function, to be used for getting more specific info from Wikidata about
   * individuals.
   *
   * @param qid The person's Wikidata QID.
   * @returns A various info and personal attributes.
   */
  suspend fun getDetailedInfo(qid: QID): PersonalInfo {

    lateinit var infoMap: PropertyMapping
    lateinit var label: Label

    val familyResp = ComplexRequester.getLabelsOrClaims(listOf(qid))
    infoMap = WikiRequestParser.parseWikidataClaims(familyResp, propertyQIDMapPersonal)[qid]!!

    val imageString = mkImage(infoMap["Wikimedia Image File"]!!)

    val PoB = getPlaceName(infoMap["PoB"]!!.getOrNull(0))
    val PoD = getPlaceName(infoMap["PoD"]!!.getOrNull(0))

    if (cache_manager.getLabel(qid) == null) {
      val labelResp = ComplexRequester.getLabelsOrClaims(listOf(qid))
      label = WikiRequestParser.parseWikidataLabels(labelResp)[qid]!!
      cache_manager.putLabel(qid, label)
    } else {
      label = cache_manager.getLabel(qid)!!
    }

    val info =
      PersonalInfo(
        imageString,
        mapOf(
          "Born" to formatDatePlaceInfo(PoB, infoMap["DoB"]),
          "Died" to formatDatePlaceInfo(PoD, infoMap["DoD"]),
        ),
        "stub", // ChatGPTDescriptionService.summarise(allInfo[qid]!!.first),
        "stub",
      )

    return info
  }

  /**
   * Returns a list of all the labels for some number of QIDs.
   *
   * @param qids A list of qids.
   * @returns A map of QID to the respective label.
   */
  suspend fun getAllLabels(qids: List<QID>): Map<QID, Label> {

    val unseenQids = mutableListOf<Label>()
    val finalLabels = mutableMapOf<QID, Label>()

    qids.forEach {
      if (cache_manager.getLabel(it) == null) {
        unseenQids.add(it)
      } else {
        finalLabels[it] = cache_manager.getLabel(it)!!
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

      qidToLabels.forEach { (qid, label) -> cache_manager.putLabel(qid, label) }
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

    if (cache_manager.getLabel(locQID) == null) {
      val locReq = ComplexRequester.getLabelsOrClaims(listOf(locQID))
      val locInfo = WikiRequestParser.parseWikidataLabels(locReq)
      val name = locInfo[locQID]!!
      cache_manager.putLabel(locQID, name)
      return name
    } else {
      return cache_manager.getLabel(locQID)!!
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

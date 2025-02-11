package org.data.services

import io.ktor.server.plugins.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.data.caches.WikiCacheManager
import org.data.models.*
import org.data.models.WikidataProperties.propertyQIDMapPersonal
import org.data.parsers.GoogleKnowledgeRequestParser
import org.data.parsers.WikiRequestParser
import org.data.requests.ComplexRequester

/** Service class for performing Wikipedia/Wikidata lookups. */
class WikiLookupService: LookupService<List<String>, List<Pair<Person, Relations>>> {

  /**
   * Query function that takes in a number of names and returns pairs of their person objects
   * and relations for family members.
   *
   * @param input A list of names.
   * @returns A list of person-relation pairs.
   * */
  override suspend fun query(input: List<String>): List<Pair<Person, Relations>> {

    val qids = mutableListOf<QID>()

    input.forEach {
      if (WikiCacheManager.getQID(it).isNullOrEmpty()) {

        val qidResp = ComplexRequester.searchWikidataForQID(it)
        val qid = WikiRequestParser.parseWikidataIDLookup(qidResp)

        if (qid == null) {
          println("QID not found: $it")
        } else {
          WikiCacheManager.putQID(it, qid)
          qids.add(qid)
        }

      } else {
        qids.add(WikiCacheManager.getQID(it)!!)
      }
    }

    return queryQIDS(qids)
  }

  /**
   * A QID-specific query function which fulfils much the same purpose of query.
   *
   * @param qids A list of QIDs.
   * @returns A list of person-relation pairs.
   * */
  suspend fun queryQIDS(qids: List<QID>): List<Pair<Person, Relations>> {

    val unseenQids = mutableListOf<QID>()

    val finalRelations = mutableListOf<Pair<Person, Relations>>()

    qids.forEach {
      if (WikiCacheManager.getProps(it) == null) {
        unseenQids.add(it)
      } else {
        val props = WikiCacheManager.getProps(it)!!
        val person = Person(it, "", props["Gender"]?.getOrNull(0) ?: "Unknown")
        finalRelations.add(Pair(person, Relations.from(props)))
      }
    }

    val claimsResp = ComplexRequester.getClaims(unseenQids)
    val mappings = WikiRequestParser.parseWikidataClaims(claimsResp)

    unseenQids.forEach {
      val props = mappings[it]!!
      WikiCacheManager.putProps(it, props)
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

    if (WikiCacheManager.getProps(qid) == null) {
      val familyResp = ComplexRequester.getClaims(listOf(qid))
      infoMap = WikiRequestParser.parseWikidataClaims(familyResp, propertyQIDMapPersonal)[qid]!!
      WikiCacheManager.putProps(qid, infoMap)
    } else {
      infoMap = WikiCacheManager.getProps(qid)!!
    }

    val imageString = mkImage(infoMap["Wikimedia Image File"]!!)

    val PoB = getPlaceName(infoMap["PoB"]!!.getOrNull(0))
    val PoD = getPlaceName(infoMap["PoD"]!!.getOrNull(0))


    if (WikiCacheManager.getLabel(qid) == null) {
      val labelResp = ComplexRequester.getLabels(listOf(qid))
      label = WikiRequestParser.parseWikidataLabels(labelResp).getOrElse(0) {"???"}
      WikiCacheManager.putLabel(qid, label)
    } else {
      label = WikiCacheManager.getLabel(qid)!!
    }

    val info =
      PersonalInfo(
        imageString,
        mapOf(
          "Born" to formatDatePlaceInfo(PoB, infoMap["DoB"]),
          "Died" to formatDatePlaceInfo(PoD, infoMap["DoD"]),
        ),
        ChatGPTDescriptionService.summarise(label),
        "stub",
      )

    return info
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

    val fmtDate: String = try {
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

    if (WikiCacheManager.getLabel(locQID) == null) {
      val locReq = ComplexRequester.getLabels(listOf(locQID))
      val locInfo = WikiRequestParser.parseWikidataLabels(locReq)
      val name = locInfo.getOrElse(0) {"???"}
      WikiCacheManager.putLabel(locQID, name)
      return name
    } else {
      return WikiCacheManager.getLabel(locQID)!!
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

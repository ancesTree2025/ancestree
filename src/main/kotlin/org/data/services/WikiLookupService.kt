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
import org.data.parsers.WikiRequestParser
import org.data.parsers.parseGoogleKnowledgeLookup
import org.data.requests.ComplexRequester

/** Service class for performing Wikipedia/Wikidata lookups. */
class WikiLookupService : LookupService<String, Pair<Person, NamedRelation>> {

  /**
   * The only exposed function, to be used for interaction with Wikipedia/Wikidata and for any sort
   * of querying.
   *
   * @param input The person's name.
   * @returns A pair of Person objects for the desired person, and their Family Relations.
   */
  override suspend fun query(input: String): Pair<Person, NamedRelation>? {
    val qid = searchForPersonsQID(input)

    if (qid.isNullOrEmpty()) {
      return null
    }

    val labelAndFamily =
      try {
        getPersonsLabelAndFamilyMembers(qid)
      } catch (e: NotFoundException) {
        return null
      }

    val personalInfo = NamedRelation.from(labelAndFamily.second)

    val person =
      Person(qid, labelAndFamily.first, labelAndFamily.second["Gender"]?.getOrNull(0) ?: "Unknown")

    return Pair(person, personalInfo)
  }

  /**
   * A new exposed function that allows for querying multiple QIDs at the same time. This is
   * massively preferable to the previous approach.
   *
   * @param input A list of input string names.
   * @returns A list of pairs of Person objects for the desired people, and their Family Relations.
   */
  suspend fun queryAll(input: List<String>): List<Pair<Person, NamedRelation>> {
    val qids = mutableListOf<QID>()

    input.forEach {
      val qid = searchForPersonsQID(it)
      if (!qid.isNullOrEmpty()) {
        qids.add(qid)
      }
    }

    val labelAndFamilyList =
      try {
        getPersonsLabelAndFamilyMembersAll(qids)
      } catch (e: Throwable) {
        return mutableListOf()
      }

    val personList = mutableListOf<Pair<Person, NamedRelation>>()

    labelAndFamilyList.forEach {
      val label = it.first
      val personalInfo = NamedRelation.from(it.second)
      val qid = WikiCacheManager.getQID(label)

      val relPerson = Person(qid!!, label, it.second["Gender"]?.getOrNull(0) ?: "Unknown")

      personList.add(Pair(relPerson, personalInfo))
    }

    return personList
  }

  /**
   * A new exposed function, to be used for getting more specific info from Wikidata about
   * individuals.
   *
   * @param qid The person's Wikidata QID.
   * @returns A various info and personal attributes.
   */
  suspend fun getDetailedInfo(qid: QID): PersonalInfo {

    val familyResponse = ComplexRequester.getLabelAndClaim(listOf(qid))
    val allInfo = WikiRequestParser.parseWikidataEntities(familyResponse, propertyQIDMapPersonal)
    val infoMap = allInfo[qid]!!.second

    val imageString = mkImage(infoMap["Wikimedia Image File"]!!)

    val PoB = getPlaceName(infoMap["PoB"]!!.getOrNull(0))
    val PoD = getPlaceName(infoMap["PoD"]!!.getOrNull(0))

    val info =
      PersonalInfo(
        imageString,
        mapOf(
          "Born" to formatDatePlaceInfo(PoB, infoMap["DoB"]),
          "Died" to formatDatePlaceInfo(PoD, infoMap["DoD"]),
        ),
        "stub", //ChatGPTDescriptionService.summarise(allInfo[qid]!!.first),
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

    var dateString = date!![0].substringBefore("T").removePrefix("+")
    var fmtDate: String

    try {
      fmtDate = LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("d/M/yyyy"))
    } catch (e: Throwable) {
      fmtDate = dateString.takeWhile { (it != '-') }
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
    val locReq = ComplexRequester.getLabelAndClaim(listOf(locQID))
    val locInfo = WikiRequestParser.parseWikidataEntities(locReq, parseClaims = false)
    val name = locInfo[locQID]!!.first
    return name
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
   * Searches for a person and returns their QID. We first query the cache and then Wikipedia.
   *
   * @param name The person's name.
   * @returns Their Wikidata QID as a string.
   */
  private suspend fun searchForPersonsQID(name: String): QID? {
    if (WikiCacheManager.getQID(name) != null) {
      return WikiCacheManager.getQID(name)!!
    }

    val response = ComplexRequester.searchWikidataForQID(name)
    val qid = WikiRequestParser.parseWikidataIDLookup(response)
    return qid
  }

  /**
   * Replaces QIDs in a family relation mapping with their labels.
   *
   * @param familyInfo A mapping of type of relation to a list of QIDs for individuals of that type.
   * @returns A human-readable mapping without QIDs.
   */
  private suspend fun replaceQIDsWithNames(familyInfo: PropertyMapping): PropertyMapping {
    /** We then select those names which don't appear in the cache, to query and store. */
    val allIds = familyInfo.values.flatten()

    if (allIds.isEmpty()) {
      return familyInfo
    }

    val unseen = allIds.filter { WikiCacheManager.getLabel(it).isNullOrEmpty() }

    if (unseen.isNotEmpty()) {
      val nameResponse = ComplexRequester.getLabelAndClaim(unseen)
      val labelClaimPair =
        WikiRequestParser.parseWikidataEntities(nameResponse, parseClaims = false)

      labelClaimPair.forEach { (qid, pair) ->
        WikiCacheManager.putQID(pair.first, qid)
        WikiCacheManager.putLabel(qid, pair.first)
      }
    }

    return familyInfo.mapValues { (_, ids) ->
      ids.map { id -> WikiCacheManager.getLabel(id) ?: "Unknown" }
    }
  }

  /**
   * Uses a person's QID to retrieve information about their family.
   *
   * @param personQID The person's QID.
   * @returns Their label and a mapping of types of relation to lists of relatives in that category.
   */
  private suspend fun getPersonsLabelAndFamilyMembers(
    personQID: QID
  ): Pair<Label, PropertyMapping> {

    val familyResponse = ComplexRequester.getLabelAndClaim(listOf(personQID))
    val labelFamilyMap = WikiRequestParser.parseWikidataEntities(familyResponse)

    val labelFamilyPair =
      labelFamilyMap[personQID]
        ?: throw NotFoundException("Label Family pair not found in singleton map.")

    val label = labelFamilyPair.first

    val familyInfo = replaceQIDsWithNames(labelFamilyPair.second)

    return Pair(label, familyInfo)
  }

  /**
   * Like the above method, but for multiple people.
   *
   * @param personQIDs A list of people's QID.
   * @returns A list of pairs, with their labels and a mapping of types of relation to relatives in
   *   that category.
   */
  private suspend fun getPersonsLabelAndFamilyMembersAll(
    personQIDs: List<QID>
  ): List<Pair<Label, PropertyMapping>> {

    val familyResponse = ComplexRequester.getLabelAndClaim(personQIDs)
    val labelFamilyMap = WikiRequestParser.parseWikidataEntities(familyResponse)

    val ls = mutableListOf<Pair<Label, PropertyMapping>>()

    personQIDs.forEach {
      val relPair = labelFamilyMap[it]!!
      ls.add(Pair(relPair.first, replaceQIDsWithNames(relPair.second)))
      WikiCacheManager.putLabel(it, relPair.first)
      WikiCacheManager.putQID(relPair.first, it)
    }

    return ls
  }

  /**
   * Fetches autocomplete results from the connected database for search input.
   *
   * @param input The part of the query that the user intends to ask.
   * @return A list of complete queries that the user may want to input.
   */
  override suspend fun fetchAutocomplete(input: String): List<String> {
    val response = ComplexRequester.getAutocompleteNames(input)

    return parseGoogleKnowledgeLookup(response)
  }
}

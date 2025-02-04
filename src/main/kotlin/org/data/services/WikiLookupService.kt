package org.data.services

import io.ktor.server.plugins.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.data.models.*
import org.data.models.WikidataProperties.propertyQIDMapPersonal
import org.data.parsers.parseWikidataEntities
import org.data.parsers.parseWikidataIDLookup
import org.data.requests.getLabelAndClaim
import org.data.requests.searchWikidataForQID

/** Service class for performing Wikipedia/Wikidata lookups. */
class WikiLookupService : LookupService<String, Pair<Person, NamedRelation>> {

  /**
   * The only exposed function, to be used for interaction with Wikipedia/Wikidata and for any sort
   * of querying.
   *
   * @param input The person's name.
   * @returns A 3-tuple of QID, Label and Family Relations.
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
   * A new exposed function, to be used for getting more specific info from Wikidata about
   * individuals.
   *
   * @param qid The person's Wikidata QID.
   * @returns A various info and personal attributes.
   */
  suspend fun getDetailedInfo(qid: QID): PersonalInfo {

    val familyResponse = getLabelAndClaim(qid)
    val allInfo = parseWikidataEntities(familyResponse, propertyQIDMapPersonal)
    val infoMap = allInfo[qid]!!.second

    val imageString = mkImage(infoMap["Wikimedia Image File"]!!)

    val PoB = getPlaceName(infoMap["PoB"]!![0])
    val PoD = getPlaceName(infoMap["PoD"]!![0])

    val info =
      PersonalInfo(
        imageString,
        mapOf(
          "Born" to formatDatePlaceInfo(PoB, infoMap["DoB"]!![0]),
          "Died" to formatDatePlaceInfo(PoD, infoMap["DoD"]!![0]),
        ),
        "stub",
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
  private fun formatDatePlaceInfo(place: String, date: String): String {
    val dateString = date.substringBefore("T").removePrefix("+")
    val formattedDate = LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("d/M/yyyy"))
    return "$place, $formattedDate."
  }

  /**
   * A simple function to query Wikidata to retrieve a place name using its QID.
   *
   * @param locQID The relevant place's QID.
   * @returns A various info and personal attributes.
   */
  private suspend fun getPlaceName(locQID: QID): Label {
    val locReq = getLabelAndClaim(locQID)
    val locInfo = parseWikidataEntities(locReq, parseClaims = false)
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
    val response = searchWikidataForQID(name)
    val qid = parseWikidataIDLookup(response)
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

    val readableNames = mutableMapOf<String, String>()

    val idsParam = allIds.joinToString("|")
    val nameResponse = getLabelAndClaim(idsParam)
    val labelClaimPair = parseWikidataEntities(nameResponse)

    labelClaimPair.forEach { (qid, pair) -> readableNames[qid] = pair.first }

    return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
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

    val familyResponse = getLabelAndClaim(personQID)
    val labelFamilyMap = parseWikidataEntities(familyResponse)

    val labelFamilyPair =
      labelFamilyMap[personQID]
        ?: throw NotFoundException("Label Family pair not found in singleton map.")

    val label = labelFamilyPair.first

    val familyInfo = replaceQIDsWithNames(labelFamilyPair.second)

    return Pair(label, familyInfo)
  }
}

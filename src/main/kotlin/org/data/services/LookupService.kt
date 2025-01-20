package org.data.services

import org.data.requests.*
import org.data.parsers.*

/**
 * Service class for performing Wikipedia/Wikidata lookups.
 */
class LookupService {

    /**
     * Searches for a person and returns their QID.
     *
     * @param name The person's name.
     * @returns Their Wikidata QID as a string.
     */
    suspend fun searchForPersonsQID(name: String): String? {
        val response = searchWikipediaForQID(name)
        return parseWikidataIDLookup(response)
    }

    /**
     * Uses a person's QID to retrieve information about their family.
     *
     * @param personQID The person's QID.
     * @returns A mapping of types of relation to lists of relatives in that category.
     */
    suspend fun getPersonsFamilyMembers(personQID: String): Map<String, List<String>> {
        val familyResponse = getFamilyInfo(personQID)
        val familyInfo = parseFamilyInfo(familyResponse)

        val nameResponse = convertWikidataIdsToNames(familyInfo)
        val readableNames = parseNames(nameResponse)

        return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
    }
}

/** Sample main function */
suspend fun main(args: Array<String>) {

    val ls = LookupService()

    val name = "Elon Musk"
    val personQID = ls.searchForPersonsQID(name)
    if (personQID.isNullOrEmpty()) {
        println("Cannot find $name.")
    }

    val familyInfo = ls.getPersonsFamilyMembers(personQID.toString())
    println("\n$name's family:")

    familyInfo.forEach{ (relation, instances) ->
        println("$relation: $instances")
    }
}

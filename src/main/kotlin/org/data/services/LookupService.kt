package org.data.services

import org.data.requests.*
import org.data.parsers.*
import org.data.caches.*

/**
 * Service class for performing Wikipedia/Wikidata lookups.
 */
class LookupService {

    /**
     * Searches for a person and returns their QID. We first query the cache and then Wikipedia.
     *
     * @param name The person's name.
     * @returns Their Wikidata QID as a string.
     */
    suspend fun searchForPersonsQID(name: String): String? {
        if (WikiCacheManager.getQID(name) != null) {
            return WikiCacheManager.getQID(name)
        }

        val response = searchWikipediaForQID(name)
        val qid = parseWikidataIDLookup(response)

        if (qid != null) {
            WikiCacheManager.putQID(name, qid)
        }

        return qid
    }

    /**
     * Uses a person's QID to retrieve information about their family.
     *
     * @param personQID The person's QID.
     * @returns A mapping of types of relation to lists of relatives in that category.
     */
    suspend fun getPersonsFamilyMembers(personQID: String): Map<String, List<String>> {

        val familyInfo: Map<String, List<String>>

        /** We first check if we have the claim stored in our cache to avoid a query. */
        if (WikiCacheManager.getClaim(personQID) != null) {
            familyInfo = parseClaimForFamily(WikiCacheManager.getClaim(personQID)!!)
        } else {
            val familyResponse = getFamilyInfo(personQID)
            familyInfo = parseFamilyInfo(familyResponse) ?: return emptyMap()
        }

        /** We then select those names which don't appear in the cache, to query and store. */
        val allIds = familyInfo.values.flatten()
        val readableNames = mutableMapOf<String, String>()
        val toQuery = mutableListOf<String>()

        allIds.forEach { id ->
            val name = WikiCacheManager.getQID(id)
            if (name == null) {
                toQuery.add(id)
            } else {
                readableNames[id] = name
            }
        }

        /** Finally, we retrieve the labels (as well as their claims for caching), and use them
         * to map the full list of IDs to names. */
        val nameResponse = convertWikidataIdsToNames(toQuery)
        val labelClaimPair = parseNames(nameResponse)


        labelClaimPair.forEach { (qid, pair) ->
            readableNames[qid] = pair.first
            WikiCacheManager.putQID(pair.first, qid)
            WikiCacheManager.putClaim(qid,  pair.second)
        }

        return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
    }
}

/** Sample main function */
suspend fun main() {

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

package org.data.services

import io.ktor.server.plugins.*
import org.data.requests.*
import org.data.parsers.*
import org.data.caches.*

/**
 * Service class for performing Wikipedia/Wikidata lookups.
 */
object LookupService {

    /**
     * Searches for a person and returns their QID. We first query the cache and then Wikipedia.
     *
     * @param name The person's name.
     * @returns Their Wikidata QID as a string.
     */
    suspend fun searchForPersonsQID(name: String): String {
        if (WikiCacheManager.getQID(name) != null) {
            return WikiCacheManager.getQID(name).toString()
        }

        val response = searchWikipediaForQID(name)
        val qid = parseWikidataIDLookup(response)

        return qid
    }

    /**
     * Uses a person's QID to retrieve information about their family.
     *
     * @param personQID The person's QID.
     * @returns A mapping of types of relation to lists of relatives in that category.
     */
    suspend fun getPersonsLabelAndFamilyMembers(personQID: String): Map<String, List<String>> {

        /** We first check if we have the claim stored in our cache to avoid a query. */
        if (WikiCacheManager.getClaim(personQID) == null) {

            /** If not, we query Wikidata to get them, and cache as relevant. */
            val familyResponse = getLabelAndClaim(personQID)
            val labelFamilyMap = parseWikidataQIDs(familyResponse)

            val labelFamilyPair = labelFamilyMap[personQID]
                ?: throw NotFoundException("Label Family pair not found in singleton map.")

            WikiCacheManager.putQID(labelFamilyPair.first, personQID)
            WikiCacheManager.putClaim(personQID, labelFamilyPair.second)
        }

        val familyInfo = parseClaimForFamily(WikiCacheManager.getClaim(personQID)!!)

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
        val idsParam = toQuery.joinToString("|")
        val nameResponse = getLabelAndClaim(idsParam)
        val labelClaimPair = parseWikidataQIDs(nameResponse)


        labelClaimPair.forEach { (qid, pair) ->
            readableNames[qid] = pair.first
            WikiCacheManager.putQID(pair.first, qid)
            WikiCacheManager.putClaim(qid,  pair.second)
        }

        return familyInfo.mapValues { (_, ids) -> ids.map { id -> readableNames[id] ?: "Unknown" } }
    }
}
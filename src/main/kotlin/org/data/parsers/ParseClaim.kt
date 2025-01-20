package org.data.parsers

import io.ktor.client.statement.*
import kotlinx.serialization.json.*

import org.data.models.FamilyProperties.familyProps

/**
 * Parses Wikidata claim, extracting the relevant QIDs of all family members.
 *
 * @param response A claim JsonObject
 * @returns A mapping of types of relation to lists of QIDs.
 */
fun parseClaimForFamily(claims: JsonObject): Map<String, List<String>> {

    val familyInfo = mutableMapOf<String, MutableList<String>>()

    claims.forEach { (prop, claimDetails) ->
        if (prop in familyProps.keys) {
            val familyMembers =
                claimDetails.jsonArray.mapNotNull { claim ->
                    claim.jsonObject["mainsnak"]
                        ?.jsonObject
                        ?.get("datavalue")
                        ?.jsonObject
                        ?.get("value")
                        ?.jsonObject
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.content
                }
            if (familyMembers.isNotEmpty()) {
                familyInfo[familyProps[prop]!!] = familyMembers.toMutableList()
            }
        }
    }

    familyProps.values.forEach { relation -> familyInfo.putIfAbsent(relation, mutableListOf()) }

    return familyInfo.mapValues { it.value.toList() }
}
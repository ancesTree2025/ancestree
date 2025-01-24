package org.data.parsers

import kotlinx.serialization.json.*
import org.data.models.*
import org.data.models.FamilyProperties.familyProps

/**
 * Parses Wikidata claim, extracting the relevant QIDs of all family members.
 *
 * @param claim A claim JsonObject
 * @returns A mapping of types of relation to lists of QIDs.
 */
fun parseClaimForFamily(claim: Claim): Relation {

  val familyInfo = mutableMapOf<String, MutableList<String>>()

  familyProps.keys.forEach { key ->
    claim[key]?.let { claimDetails ->
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
        familyInfo[familyProps[key]!!] = familyMembers.toMutableList()
      }
    }
  }

  familyProps.values.forEach { relation -> familyInfo.putIfAbsent(relation, mutableListOf()) }

  return familyInfo.mapValues { it.value.toList() }
}

package org.graph

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class FamilyRelation(val FirstName: String, val LastName: String) {

    val children = mutableListOf<FamilyRelation>()
    val spouses = mutableListOf<FamilyRelation>()
    val siblings = mutableListOf<FamilyRelation>()


    fun addChild(child: FamilyRelation) {
        children.add(child)
    }

    fun addSpouse(spouse: FamilyRelation) {
        spouses.add(spouse)
    }

    fun addSibling(sibling: FamilyRelation) {
        siblings.add(sibling)
    }
}

//fun main() {
//    val json = Json.encodeToString(FamilyRelation("Henry", "The 8th"))
//}
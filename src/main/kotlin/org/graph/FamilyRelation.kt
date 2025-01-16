package org.graph

import kotlinx.serialization.Serializable

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
package org.domain.models

data class Node(val FirstName: String, val LastName: String) {

    val children = mutableListOf<Node>()
    val spouses = mutableListOf<Node>()
    val siblings = mutableListOf<Node>()


    fun addChild(child: Node) {
        children.add(child)
    }

    fun addSpouse(spouse: Node) {
        spouses.add(spouse)
    }

    fun addSibling(sibling: Node) {
        siblings.add(sibling)
    }
}
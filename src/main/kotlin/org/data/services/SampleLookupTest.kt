package org.data.services

/** Sample main function */
suspend fun main() {

    val name = "Maye Musk"
    val tuple3 = LookupService.query(name)

    val qid = tuple3.first
    val label = tuple3.second.first
    val relation = tuple3.second.second

    println("Official Name: $label\nQID: $qid")
    relation.forEach{ (type, members) ->
        println("-- $type: ${members.joinToString(" — ")}")
    }
}

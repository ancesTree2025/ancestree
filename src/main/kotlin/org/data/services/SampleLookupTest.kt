package org.data.services

import org.data.caches.WikiCacheManager

/** Sample main function */
suspend fun main() {

    val name = "Elon Musk"
    val personQID = LookupService.searchForPersonsQID(name)
    val familyInfo = LookupService.getPersonsLabelAndFamilyMembers(personQID)
    println("\n$name's family:")

    familyInfo.forEach{ (relation, instances) ->
        println("$relation: $instances")
    }
}

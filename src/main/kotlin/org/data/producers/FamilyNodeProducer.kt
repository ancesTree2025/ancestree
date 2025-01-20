package org.data.producers

import org.data.models.FamilyData
import org.data.models.FamilyProperties
import org.data.services.LookupService
import org.domain.models.*
import org.domain.producers.NodeProducer

class FamilyNodeProducer : NodeProducer<String, FamilyData> {



  override suspend fun produce(input: String): Node<FamilyData> {
//    val inputQID = LookupService.searchForPersonsQID(input)
//    val family = LookupService.getPersonsFamilyMembers(inputQID)

//    val familyInfo = FamilyData(inputQID, )
//
//    val familyNode = Node<>
//
    TODO()


  }

  override suspend fun produceAll(input: String): List<Node<FamilyData>> {
    return mutableListOf()
  }
}
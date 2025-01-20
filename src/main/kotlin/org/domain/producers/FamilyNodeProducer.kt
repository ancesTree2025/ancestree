package org.domain.producers

import org.data.FamilyProperties
import org.domain.models.Node

class FamilyNodeProducer : NodeProducer<String, FamilyProperties> {
  override fun produce(input: String): Node<Map<String, List<String>>> {
    TODO("Not yet implemented")
  }
}
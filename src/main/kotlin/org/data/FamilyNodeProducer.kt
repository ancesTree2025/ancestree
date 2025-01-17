package org.data

import org.domain.NodeProducer
import org.domain.models.Node

typealias FamilyProperties = Map<String, List<String>>

class FamilyNodeProducer : NodeProducer<String, FamilyProperties> {
  override fun produce(input: String): Node<Map<String, List<String>>> {
    TODO("Not yet implemented")
  }
}

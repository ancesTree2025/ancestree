package org.domain.producers

import org.domain.models.Node

interface NodeProducer<Input, Output> {
  suspend fun produce(input: Input): Node<Output>

  suspend fun produceAll(input: Input): List<Node<Output>>
}

package org.domain

import org.domain.models.Node

interface NodeProducer<Input, Output> {
  fun produce(input: Input): Node<Output>
}
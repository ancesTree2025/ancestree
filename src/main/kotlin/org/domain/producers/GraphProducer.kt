package org.domain.producers

import org.domain.models.Graph
import org.domain.models.Node

interface GraphProducer<Input, Output> {
  suspend fun produceGraph(root: Input): Graph<Output>

  suspend fun produceNode(query: Input, depth: Int): Node<Output>
}

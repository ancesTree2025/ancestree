package org.domain.producers

import org.domain.models.Graph

interface GraphProducer<Input, Output> {
  suspend fun produceGraph(root: Input): Graph<Output>
}

package org.domain.models

data class Node<T>(
  val data: T,
  val id: String,
  val depth: Int
)

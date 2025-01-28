package org.domain.models

import kotlinx.serialization.Serializable

@Serializable data class Node<T>(val data: T, val id: String, val depth: Int)

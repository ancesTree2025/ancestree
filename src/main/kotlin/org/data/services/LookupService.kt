package org.data.services

interface LookupService<I, O> {
  suspend fun query(input: I): O
}
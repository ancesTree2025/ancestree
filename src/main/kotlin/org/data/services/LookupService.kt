package org.data.services

interface LookupService<I, O> {
  suspend fun query(input: I): O?

  suspend fun fetchAutocomplete(input: I): List<I>
}

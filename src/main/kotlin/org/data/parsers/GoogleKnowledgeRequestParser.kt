package org.data.parsers

import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.data.models.ItemListElement

object GoogleKnowledgeRequestParser {

  /**
   * Parses Wikipedia ID Lookup responses, extracting the relevant QID.
   *
   * @param response The HTTP response from Wikipedia.
   * @returns A single parsed QID, as a string.
   */
  suspend fun parseGoogleKnowledgeLookup(response: HttpResponse): List<String> {
    val json = Json { ignoreUnknownKeys = true }
    val result = json.decodeFromString<ItemListElement>(response.bodyAsText())

    val autocompleteList = mutableListOf<String>()

    result.itemListElement.forEach { resultObject -> autocompleteList.add(resultObject.result.name) }

    return autocompleteList
  }
}
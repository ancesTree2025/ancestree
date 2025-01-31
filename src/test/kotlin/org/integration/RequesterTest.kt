package org.integration

import io.ktor.client.statement.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.data.models.PagesResponse
import org.data.models.WikidataResponse
import org.data.requests.BaseRequester
import org.data.requests.ComplexRequester

class RequesterTest {
  private val timeout = 120.seconds
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `can successfully make a request to the Wikipedia API`() =
    runTest(timeout = timeout) {
      val httpResponse = BaseRequester.doWikipediaRequest("") {}
      assertEquals(200, httpResponse.status.value)
    }

  @Test
  fun `can successfully make a request to the Wikidata API`() =
    runTest(timeout = timeout) {
      val httpResponse = BaseRequester.doWikidataRequest("") {}
      assertEquals(200, httpResponse.status.value)
    }

  @Test
  fun `can successfully fetch a QID from Wikidata given a name`() =
    runTest(timeout = timeout) {
      val httpResponse = ComplexRequester.searchWikidataForQID("Elon Musk")
      val result = json.decodeFromString<PagesResponse>(httpResponse.bodyAsText())

      assertEquals(200, httpResponse.status.value)

      // Check that required fields are present in the JSON
      assertNotNull(result.query)
      assertNotNull(result.query?.pages)
      assert(result.query?.pages?.isNotEmpty() ?: false)
    }

  @Test
  fun `can successfully fetch a label and claims from Wikidata given a QID`() =
    runTest(timeout = timeout) {
      val httpResponse = ComplexRequester.getLabelAndClaim(listOf("Q317521"))
      val result = json.decodeFromString<WikidataResponse>(httpResponse.bodyAsText())

      assertEquals(200, httpResponse.status.value)

      // Check that required fields are present in the JSON
      assert(result.entities.isNotEmpty())
      assertNotNull(result.entities["Q317521"])
      assertNotNull(result.entities["Q317521"]?.labels?.en)
    }
}

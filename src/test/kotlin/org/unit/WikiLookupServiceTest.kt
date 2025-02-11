package org.unit

import io.mockk.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.MockHttpResponse
import org.data.parsers.GoogleKnowledgeRequestParser
import org.data.parsers.WikiRequestParser
import org.data.requests.BaseRequester
import org.data.requests.ComplexRequester
import org.data.requests.HttpClientProvider
import org.data.services.WikiLookupService

class WikiLookupServiceTest {
  private val timeout = 60.seconds

  @BeforeTest
  fun setUp() {
    mockkObject(WikiRequestParser)
    mockkObject(GoogleKnowledgeRequestParser)
    mockkObject(BaseRequester)
    mockkObject(ComplexRequester)
    mockkObject(HttpClientProvider)

    coEvery { BaseRequester.doWikipediaRequest(any(), any()) } returns MockHttpResponse()
    coEvery { BaseRequester.doWikidataRequest(any(), any()) } returns MockHttpResponse()
    coEvery { BaseRequester.doGoogleKnowledgeRequest(any()) } returns MockHttpResponse()
    coEvery { ComplexRequester.getLabelAndClaim(any()) } returns MockHttpResponse()
    coEvery { ComplexRequester.searchWikidataForQID(any()) } returns MockHttpResponse()
    coEvery { ComplexRequester.getAutocompleteNames(any()) } returns MockHttpResponse()
  }

  @Test
  fun `querying a person's name returns their QID, label and relations`() =
    runTest(timeout = timeout) {
      coEvery { WikiRequestParser.parseWikidataIDLookup(any()) } returns "Q317521"
      coEvery { WikiRequestParser.parseWikidataClaims(any()) } returns
        mapOf(Pair("Q317521", Pair("Elon Musk", mapOf())))

      val response = WikiLookupService().query("Elon Musk")
      val person = response?.first
      val relation = response?.second

      assertNotNull(response)
      assertNotNull(person)
      assertNotNull(relation)
      assertEquals("Q317521", person.id)
      assertEquals("Elon Musk", person.name)
      // TODO: test rest of query response

      coVerify(inverse = true) {
        HttpClientProvider.httpClient
      } // unit tests should not be making requests
    }

  @AfterTest
  fun cleanup() {
    unmockkObject(WikiRequestParser)
    unmockkObject(GoogleKnowledgeRequestParser)
    unmockkObject(BaseRequester)
    unmockkObject(ComplexRequester)
    unmockkObject(HttpClientProvider)
  }
}

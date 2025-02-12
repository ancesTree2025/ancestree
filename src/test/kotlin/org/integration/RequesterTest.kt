package org.integration

import io.ktor.client.statement.*


// class RequesterTest {
//  private val timeout = 60.seconds
//  private val json = Json { ignoreUnknownKeys = true }
//
//  /*
//  Bad requests but we can check that there is a connection. Returns roughly the following JSON
// structure.
//
//    error:
//      code: "badvalue"
//      info: "Unrecognized value for parameter "action": ."
//      *: "See https://en.wikipedia.org/w/api.php for API usage..."
//    servedby: ...
//
//   */
//
//  @Test
//  fun `can successfully connect to the Wikidata API`() =
//    runTest(timeout = timeout) {
//      val httpResponse = BaseRequester.doWikidataRequest("") {}
//      assertEquals(200, httpResponse.status.value)
//    }
//
//  // These JSONs are serialised to type PagesResponse and WikidataResponse
//
//  @Test
//  fun `can successfully fetch a QID from Wikidata given a name`() =
//    runTest(timeout = timeout) {
//      val httpResponse = ComplexRequester.searchWikidataForQID("Elon Musk")
//      val result = json.decodeFromString<PagesResponse>(httpResponse.bodyAsText())
//
//      assertEquals(200, httpResponse.status.value)
//
//      // Check that required fields are present in the JSON
//      assertNotNull(result.query)
//      assertNotNull(result.query?.pages)
//      assert(result.query?.pages?.isNotEmpty() ?: false)
//      assert(httpResponse.bodyAsText().contains("Q317521"))
//    }
//
//  @Test
//  fun `can successfully fetch a label and claims from Wikidata given a QID`() =
//    runTest(timeout = timeout) {
//      val httpResponse = ComplexRequester.getLabelsOrClaims(listOf("Q317521")) // Elon Musk
//      val result = json.decodeFromString<WikidataResponse>(httpResponse.bodyAsText())
//
//      assertEquals(200, httpResponse.status.value)
//
//      // Check that required fields are present in the JSON
//      assert(result.entities.isNotEmpty())
//      assertNotNull(result.entities["Q317521"])
//      assertNotNull(result.entities["Q317521"]?.labels?.en)
//      assert(httpResponse.bodyAsText().contains("Elon Musk"))
//      assertNotNull(result.entities["Q317521"]?.claims)
//    }
// }

package org.integration

import io.ktor.client.statement.*
import kotlin.test.*


//
// class RequestParserTest {
//  private val timeout = 60.seconds
//  private lateinit var response: HttpResponse
//
//  @BeforeTest
//  fun setUp(): Unit = runBlocking {
//    response = ComplexRequester.getLabelAndClaim(listOf("Q317521")) // Elon Musk
//  }
//
//  @Test
//  fun `can parse wikidata request response into QID`() =
//    runTest(timeout = timeout) {
//      val response = ComplexRequester.searchWikidataForQID("Elon Musk")
//      val qid = WikiRequestParser.parseWikidataIDLookup(response)
//      assertEquals("Q317521", qid)
//    }
//
//  @Test
//  fun `can parse wikidata request response into one label and its claims`() =
//    runTest(timeout = timeout) {
//      val labelClaimPair = WikiRequestParser.parseWikidataClaims(response)
//      assert(labelClaimPair.size == 1)
//      assertEquals("Elon Musk", labelClaimPair["Q317521"]?.first)
//      assertNotNull(labelClaimPair["Q317521"]?.second)
//    }
//
//  @Test
//  fun `can parse wikidata request response for personal props`() =
//    runTest(timeout = timeout) {
//      val labelClaimPair = WikiRequestParser.parseWikidataClaims(response, propertyQIDMapPersonal)
//      val claim = labelClaimPair["Q317521"]?.second
//      assertEquals("Elon Musk", labelClaimPair["Q317521"]?.first)
//      for ((_, prop) in propertyQIDMapPersonal) {
//        assert(claim?.contains(prop) ?: false)
//      }
//    }
//
//  @Test
//  fun `can parse wikidata response for location name`() =
//    runTest(timeout = timeout) {
//      val response = ComplexRequester.getLabelAndClaim(listOf("Q189022"))
//      val locInfo = WikiRequestParser.parseWikidataClaims(response, parseClaims = false)
//      assertEquals("Imperial College London", locInfo["Q189022"]?.first)
//    }
// }

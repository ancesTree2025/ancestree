package org

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test

class ApplicationTest {

  @Test
  fun testRoot() = testApplication {
    // application { module() }
    // client.get("/").apply { assertEquals(HttpStatusCode.OK, status) }
  }
}

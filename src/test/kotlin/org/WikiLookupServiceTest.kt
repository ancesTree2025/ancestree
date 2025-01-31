package org

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class WikiLookupServiceTest {
  @Test fun testTest() = runTest { assertEquals("foo", "foo") }
}

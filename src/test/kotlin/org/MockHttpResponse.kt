package org

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import io.mockk.mockk
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

class MockHttpResponse(
  override val call: HttpClientCall = mockk(relaxed = true),
  override val status: HttpStatusCode = HttpStatusCode.OK,
  override val version: HttpProtocolVersion = HttpProtocolVersion.HTTP_1_1,
  override val headers: Headers = headersOf(),
  override val requestTime: GMTDate = GMTDate(),
  override val responseTime: GMTDate = GMTDate(),
  override val coroutineContext: CoroutineContext = Dispatchers.IO,
  private val content: ByteReadChannel = ByteReadChannel.Empty,
) : HttpResponse() {

  @InternalAPI
  override val rawContent: ByteReadChannel
    get() = content
}

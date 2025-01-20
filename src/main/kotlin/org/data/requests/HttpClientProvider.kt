package org.data.requests

import io.ktor.client.*

/**
 * Lazily generated HTTP client provider, ensuring only
 * a single instance exists.
 */
object HttpClientProvider {
    val httpClient: HttpClient by lazy { HttpClient() }
}

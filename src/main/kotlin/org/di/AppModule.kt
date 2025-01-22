package org.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bindMultiton

val appDI = DI { import(appModule) }

val appModule =
  DI.Module("AppModule") {
    bindMultiton<String, HttpClient> { url -> HttpClient(CIO) { configure(url) } }
  }

fun HttpClientConfig<CIOEngineConfig>.configure(url: String) {
  install(DefaultRequest) { url(url) }
  install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

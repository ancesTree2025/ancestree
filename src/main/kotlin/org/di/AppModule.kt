package org.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json
import org.data.models.Person
import org.data.producers.FamilyGraphProducer
import org.domain.producers.GraphProducer
import org.kodein.di.DI
import org.kodein.di.bindMultiton
import org.kodein.di.bindSingleton

//val appDI = DI { import(appModule) }

val appModule by lazy {
  DI.Module("AppModule") {
    bindMultiton<String, HttpClient> { url -> HttpClient(CIO) { configure(url) } }

    bindSingleton<GraphProducer<String, Person>> { FamilyGraphProducer() }
  }
}

fun HttpClientConfig<CIOEngineConfig>.configure(url: String) {
  install(DefaultRequest) { url(url) }
  install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

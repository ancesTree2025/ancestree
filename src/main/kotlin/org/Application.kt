package org

import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.CORS
import org.core.configureRouting
import org.core.configureSerialization
import org.di.appModule
import org.kodein.di.ktor.di

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
  di { import(appModule) }
  install(CORS) {
    val allowedHosts =
      with(System.getenv("ALLOWED_HOSTS") ?: "") { this.split(",").map(String::trim) }

    allowedHosts.forEach { allowHost(it) }
  }
  configureSerialization()
  configureRouting()
}

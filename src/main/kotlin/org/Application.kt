package org

import io.ktor.server.application.*
import org.core.configureRouting
import org.core.configureSerialization

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
  configureSerialization()
  configureRouting()
}

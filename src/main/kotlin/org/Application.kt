package org

import io.ktor.server.application.*
import org.core.configureRouting
import org.core.configureSerialization
import org.di.appModule
import org.kodein.di.ktor.di

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    di {
        import(appModule)
    }
    configureSerialization()
    configureRouting()
}

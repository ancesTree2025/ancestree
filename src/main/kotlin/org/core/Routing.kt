package org.core

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.data.FamilyNodeProducer
import org.data.FamilyProperties
import org.domain.NodeProducer
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

fun Application.configureRouting() {
    val clientFactory by closestDI().factory<String, HttpClient>()
    val nodeProducer by closestDI().instance<NodeProducer<String, FamilyProperties>>()

    routing {
        get("/{name}") {
            val name = call.parameters["name"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "name is required. Nothing was passed")

            call.respond(HttpStatusCode.OK, nodeProducer.produce(name))
        }
    }
}

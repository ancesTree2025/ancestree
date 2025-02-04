package org.core

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.data.models.Person
import org.data.services.WikiLookupService
import org.domain.models.AutocompleteResponse
import org.domain.producers.GraphProducer
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

fun Application.configureRouting() {
  val clientFactory by closestDI().factory<String, HttpClient>()
  val graphProducer by closestDI().instance<GraphProducer<String, Person>>()
  val lookupService = WikiLookupService()

  routing {
    get("/{name}") {
      val name =
        call.parameters["name"]
          ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "name is required. Nothing was passed",
          )

      val graph = graphProducer.produceGraph(name)
      if (graph.isEmpty()) return@get call.respond(HttpStatusCode.NoContent)

      call.respond(graph)
    }

    get("/search/{searchQuery}") {
      val query =
        call.parameters["searchQuery"] ?: return@get call.respond(HttpStatusCode.BadRequest)
      call.respond(AutocompleteResponse(query, lookupService.fetchAutocomplete(query)))
    }
  }
}

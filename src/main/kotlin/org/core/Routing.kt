package org.core

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.data.models.Person
import org.data.services.GraphTrawlService
import org.data.services.InfoQueryBuilder
import org.data.services.WikiLookupService
import org.domain.models.AutocompleteResponse
import org.domain.producers.GraphProducer
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

fun Application.configureRouting() {
  val DEFAULT_GRAPH_WIDTH = 4
  val DEFAULT_GRAPH_HEIGHT = 4

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

      val width = call.request.queryParameters["width"]?.toIntOrNull() ?: DEFAULT_GRAPH_WIDTH
      val height = call.request.queryParameters["height"]?.toIntOrNull() ?: DEFAULT_GRAPH_HEIGHT

      val graph = graphProducer.produceGraph(name, width, height)
      if (graph.isEmpty()) return@get call.respond(HttpStatusCode.NoContent)

      call.respond(graph)
    }

    get("/search/{searchQuery}") {
      val query =
        call.parameters["searchQuery"] ?: return@get call.respond(HttpStatusCode.BadRequest)
      call.respond(AutocompleteResponse(query, lookupService.fetchAutocomplete(query)))
    }
  }

  routing {
    get("/info") {
      val qid =
        call.request.queryParameters["qid"]
          ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "qid is required. Nothing was passed",
          )

      val paramList =
        call.parameters.getAll("params[]")
          ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "params is required. Nothing was passed",
          )

      println("Parameters: $paramList")

      val queryParams = InfoQueryBuilder()

      for (param in paramList) {
        when (param) {
          "image" -> queryParams.withImage()
          "birth" -> queryParams.withBirth()
          "death" -> queryParams.withDeath()
          "description" -> queryParams.withDescription()
          "wikiLink" -> queryParams.withWikiLink()
          "rcoords" -> queryParams.withResCoords()
          "residence" -> queryParams.withResidence()
          "bcoords" -> queryParams.withBCoords()
          "dcoords" -> queryParams.withDCoords()
          "office" -> queryParams.withOffice()
          "all" -> queryParams.withAll()
        }
      }

      val wikiLookupService = WikiLookupService()
      val personInfo = wikiLookupService.getDetailedInfo(qid, queryParams)
      call.respond(personInfo)
    }
  }

  routing {
    get("/relation") {
      val orig =
        call.request.queryParameters["orig"]
          ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Origin node is required. Nothing was passed",
          )

      val dest =
        call.request.queryParameters["dest"]
          ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Dest is required. Nothing was passed",
          )

      val graphTrawlService = GraphTrawlService()
      val linkInfo = graphTrawlService.getRelation(orig, dest)
      call.respond(linkInfo)
    }
  }
}

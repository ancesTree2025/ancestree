package org.data.services

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.data.models.GPTResponse
import org.data.models.Label
import org.data.requests.HttpClientProvider

object GPTLookupService : LookupService<Label, String> {

  private val dotenv = dotenv()
  private val apiKey = dotenv["OPENAI_KEY"]
  private val url = "https://api.openai.com/v1/chat/completions"

  override suspend fun query(input: List<Label>): List<String> {

    val responses = mutableListOf<String>()

    input.forEach { person ->
      val requestBody = buildJsonObject {
        put("model", "gpt-4o-mini")
        putJsonArray("messages") {
          addJsonObject {
            put("role", "system")
            put(
              "content",
              "Provide a simple summary of the given person in no more than 3 sentences. Talk about what they are" +
                "best known for and important facts about their life.",
            )
          }
          addJsonObject {
            put("role", "user")
            put("content", person)
          }
        }
        put("temperature", 0)
      }

      val response =
        HttpClientProvider.httpClient.post(url) {
          contentType(ContentType.Application.Json)
          header("Authorization", "Bearer $apiKey")
          setBody(requestBody.toString())
        }

      val json = Json { ignoreUnknownKeys = true }
      val result = json.decodeFromString<GPTResponse>(response.bodyAsText())
      responses.add(result.choices[0].message.content)
    }
    return responses
  }
}

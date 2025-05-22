package data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class KickstarterApiClient(
    private var authToken: String
) {
    private val logger = LoggerFactory.getLogger("KickstarterApiClient")
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(Logging) { logger = Logger.DEFAULT; level = LogLevel.INFO }
        install(HttpTimeout) { requestTimeoutMillis = 60000 }
    }

    private val queries = KickstarterQueries

    suspend fun fetchProjects(cursor: String?): JsonObject {
        val variables = buildJsonObject {
            put("sort", JsonPrimitive("MAGIC"))
            put("first", JsonPrimitive(15))
            cursor?.let { put("cursor", JsonPrimitive(it)) }
        }
        return postGql("FetchProjects", queries.fetchProjectsQuery(), variables)
    }

    suspend fun fetchProjectDetails(slug: String): JsonObject {
        val variables = buildJsonObject { put("slug", JsonPrimitive(slug)) }
        return postGql("FetchProject", queries.fetchProjectDetailsQuery(), variables)
    }

    suspend fun fetchProjectRewards(slug: String): JsonObject {
        val variables = buildJsonObject { put("slug", JsonPrimitive(slug)) }
        return postGql("FetchProjectRewards", queries.fetchProjectRewardsQuery(), variables)
    }

    suspend fun fetchCreatorDetails(slug: String): JsonObject {
        val variables = buildJsonObject { put("slug", JsonPrimitive(slug)) }
        return postGql("ProjectCreatorDetails", queries.fetchCreatorDetailsQuery(), variables)
    }

    private suspend fun postGql(operationName: String, query: String, variables: JsonObject): JsonObject {
        val payload = buildJsonObject {
            put("operationName", JsonPrimitive(operationName))
            put("variables", variables)
            put("query", JsonPrimitive(query))
        }
        val resp = client.post("https://www.kickstarter.com/graph") {
            contentType(ContentType.Application.Json)
            header("Authorization", authToken)
            header("User-Agent", "Kickstarter Android Mobile Variant/externalRelease Code/2014150939 Version/3.31.1")
            setBody(payload)
        }
        return resp.body<JsonObject>()
    }
}

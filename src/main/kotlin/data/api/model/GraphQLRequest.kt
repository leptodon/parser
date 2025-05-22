package data.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GraphQLRequest(
    val operationName: String,
    val variables: JsonObject,
    val query: String
)

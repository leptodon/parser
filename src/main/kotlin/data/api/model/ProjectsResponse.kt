package data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectsResponse(
    val data: ProjectsData
)

@Serializable
data class ProjectsData(
    val projects: ProjectsConnection
)

@Serializable
data class ProjectsConnection(
    val edges: List<ProjectEdge>,
    val pageInfo: PageInfo,
    val totalCount: Int
)

@Serializable
data class ProjectEdge(
    val cursor: String,
    val node: ProjectNode
)

@Serializable
data class ProjectNode(
    @SerialName("__typename") val typename: String? = null,
    val id: String,
    val pid: Long? = null,
    val name: String,
    val slug: String,
    val description: String,
    val state: String,
    val category: CategoryNode,
    val country: CountryNode,
    val location: LocationNode? = null,
    val goal: MoneyNode,
    val pledged: MoneyNode,
    @SerialName("backersCount") val backersCount: Int,
    val creator: CreatorNode,
    @SerialName("launchedAt") val launchedAt: Long? = null, // Unix timestamp в секундах
    @SerialName("deadlineAt") val deadlineAt: Long? = null, // Сделали nullable - Unix timestamp в секундах
    @SerialName("createdAt") val createdAt: Long? = null, // Unix timestamp в секундах
    @SerialName("isProjectWeLove") val isProjectWeLove: Boolean = false,
    @SerialName("percentFunded") val percentFunded: Int? = null,
    @SerialName("isLaunched") val isLaunched: Boolean? = null,
    @SerialName("isPledgeOverTimeAllowed") val isPledgeOverTimeAllowed: Boolean? = null
)

@Serializable
data class CategoryNode(
    val name: String,
    val parentCategory: ParentCategoryNode? = null
)

@Serializable
data class ParentCategoryNode(
    val name: String
)

@Serializable
data class CountryNode(
    val code: String,
    val name: String
)

@Serializable
data class LocationNode(
    @SerialName("displayableName") val displayableName: String
)

@Serializable
data class MoneyNode(
    val amount: String, // Приходит как строка
    val currency: String,
    val symbol: String
)

@Serializable
data class CreatorNode(
    val name: String,
    val id: String? = null
)

@Serializable
data class PageInfo(
    @SerialName("hasPreviousPage") val hasPreviousPage: Boolean,
    @SerialName("hasNextPage") val hasNextPage: Boolean,
    @SerialName("startCursor") val startCursor: String? = null,
    @SerialName("endCursor") val endCursor: String? = null
)
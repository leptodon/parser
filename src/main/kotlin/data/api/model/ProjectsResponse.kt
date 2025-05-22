package data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectsResponse(
    val data: ProjectsData
)

@Serializable
data class ProjectsData(
    val projects: Projects
)

@Serializable
data class Projects(
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
data class PageInfo(
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean,
    val startCursor: String?,
    val endCursor: String?
)

@Serializable
data class ProjectNode(
    val __typename: String,
    val id: String,
    val pid: Long? = null,
    val name: String,
    val slug: String,
    val description: String,
    val backersCount: Int,
    val isLaunched: Boolean,
    val isPledgeOverTimeAllowed: Boolean,
    val category: CategoryNode,
    val country: CountryNode,
    val createdAt: Long,
    val creator: CreatorNode,
    val deadlineAt: Long,
    val goal: MoneyNode,
    val pledged: MoneyNode,
    val percentFunded: Int? = null,
    val launchedAt: Long? = null,
    val location: LocationNode? = null,
    val isProjectWeLove: Boolean,
    val state: String
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
data class CreatorNode(
    val name: String,
    val id: String,
    val backingsCount: Int? = null,
    val launchedProjects: LaunchedProjects? = null
)

@Serializable
data class LaunchedProjects(
    val totalCount: Int
)

@Serializable
data class MoneyNode(
    val amount: String,
    val currency: String,
    val symbol: String
)

@Serializable
data class LocationNode(
    val displayableName: String
)

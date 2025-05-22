package data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectDetailsResponse(
    val data: ProjectDetailsData
)

@Serializable
data class ProjectDetailsData(
    val project: ProjectDetailsNode
)

@Serializable
data class ProjectDetailsNode(
    val __typename: String,
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val backersCount: Int,
    val minPledge: Double? = null,
    val isLaunched: Boolean,
    val category: CategoryNode,
    val commentsCount: Int? = null,
    val country: CountryNode,
    val createdAt: Long,
    val creator: CreatorDetailsNode,
    val currency: String,
    val deadlineAt: Long,
    val goal: MoneyNode,
    val pledged: MoneyNode,
    val launchedAt: Long? = null,
    val location: LocationNode? = null,
    val rewards: RewardsConnection,
    val risks: String? = null,
    val story: String? = null,
    val isProjectWeLove: Boolean,
    val state: String,
    val stateChangedAt: Long? = null,
    val posts: PostsConnection? = null,
    val video: VideoNode? = null,
    val faqs: FaqsConnection? = null,
    val environmentalCommitments: List<EnvironmentalCommitment>? = null
)

@Serializable
data class CreatorDetailsNode(
    val name: String,
    val backingsCount: Int? = null,
    val launchedProjects: LaunchedProjects? = null
)

@Serializable
data class RewardsConnection(
    val nodes: List<RewardNode>
)

@Serializable
data class RewardNode(
    val id: String,
    val name: String,
    val backersCount: Int,
    val description: String,
    val estimatedDeliveryOn: String? = null,
    val available: Boolean,
    val amount: MoneyNode,
    val shippingPreference: String,
    val remainingQuantity: Int? = null,
    val limit: Int? = null,
    val limitPerBacker: Int? = null,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val simpleShippingRulesExpanded: List<ShippingRule>? = null
)

@Serializable
data class ShippingRule(
    val locationName: String
)

@Serializable
data class PostsConnection(
    val totalCount: Int
)

@Serializable
data class VideoNode(
    val videoSources: VideoSources? = null
)

@Serializable
data class VideoSources(
    val high: VideoSource? = null
)

@Serializable
data class VideoSource(
    val src: String
)

@Serializable
data class FaqsConnection(
    val nodes: List<FaqNode>
)

@Serializable
data class FaqNode(
    val id: String
)

@Serializable
data class EnvironmentalCommitment(
    val commitmentCategory: String,
    val description: String
)

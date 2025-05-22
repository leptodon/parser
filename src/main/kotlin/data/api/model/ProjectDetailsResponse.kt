package data.api.model

import kotlinx.serialization.SerialName
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
    @SerialName("__typename") val typename: String? = null,
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val story: String? = null,
    val risks: String? = null,
    val state: String,
    val category: CategoryNode,
    val country: CountryNode,
    val location: LocationNode? = null,
    val goal: MoneyNode,
    val pledged: MoneyNode,
    @SerialName("backersCount") val backersCount: Int,
    val creator: CreatorDetailsNode,
    @SerialName("launchedAt") val launchedAt: Long? = null, // Unix timestamp в секундах
    @SerialName("deadlineAt") val deadlineAt: Long? = null, // Сделали nullable - Unix timestamp в секундах
    @SerialName("createdAt") val createdAt: Long? = null, // Unix timestamp в секундах
    @SerialName("isProjectWeLove") val isProjectWeLove: Boolean = false,
    @SerialName("isLaunched") val isLaunched: Boolean? = null,
    @SerialName("minPledge") val minPledge: String? = null,
    val currency: String? = null,
    @SerialName("commentsCount") val commentsCount: Int? = null,
    @SerialName("stateChangedAt") val stateChangedAt: Long? = null,
    val rewards: RewardsConnection,
    val video: VideoNode? = null,
    val posts: PostsConnection? = null,
    val faqs: FaqsConnection? = null,
    @SerialName("environmentalCommitments") val environmentalCommitments: List<EnvironmentalCommitmentNode>? = null
)

@Serializable
data class CreatorDetailsNode(
    val name: String,
    val id: String? = null,
    @SerialName("backingsCount") val backingsCount: Int? = null,
    @SerialName("launchedProjects") val launchedProjects: LaunchedProjectsNode? = null
)

@Serializable
data class LaunchedProjectsNode(
    val totalCount: Int
)

@Serializable
data class RewardsConnection(
    val nodes: List<RewardNode>
)

@Serializable
data class RewardNode(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("backersCount") val backersCount: Int = 0,
    val amount: MoneyNode,
    val available: Boolean? = null,
    @SerialName("estimatedDeliveryOn") val estimatedDeliveryOn: String? = null,
    @SerialName("shippingPreference") val shippingPreference: String? = null,
    @SerialName("remainingQuantity") val remainingQuantity: Int? = null,
    val limit: Int? = null,
    @SerialName("limitPerBacker") val limitPerBacker: Int? = null,
    @SerialName("startsAt") val startsAt: Long? = null, // Unix timestamp
    @SerialName("endsAt") val endsAt: Long? = null, // Unix timestamp
    @SerialName("simpleShippingRulesExpanded") val simpleShippingRulesExpanded: List<ShippingRuleNode>? = null
)

@Serializable
data class ShippingRuleNode(
    @SerialName("locationName") val locationName: String
)

@Serializable
data class VideoNode(
    @SerialName("videoSources") val videoSources: VideoSourcesNode? = null
)

@Serializable
data class VideoSourcesNode(
    val high: HighQualityVideoNode? = null
)

@Serializable
data class HighQualityVideoNode(
    val src: String
)

@Serializable
data class PostsConnection(
    val totalCount: Int
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
data class EnvironmentalCommitmentNode(
    @SerialName("commitmentCategory") val commitmentCategory: String,
    val description: String
)
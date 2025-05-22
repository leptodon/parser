package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val name: String,
    val slug: String,
    val parentCategory: ParentCategory? = null
)
@Serializable
data class ParentCategory(
    val name: String,
    val slug: String
)

@Serializable
data class Money(
    val amount: Double,
    val currency: String,
    val symbol: String
)

@Serializable
data class Location(
    val displayableName: String?,
    val country: String?,
    val name: String?
)

@Serializable
data class Creator(
    val name: String,
    val backingsCount: Int = 0,
    val launchedProjects: Int = 0
)

@Serializable
data class Reward(
    val id: String,
    val name: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val backersCount: Int,
    val available: Boolean,
    val estimatedDeliveryOn: String?
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val slug: String,
    val url: String,
    val description: String,
    val story: String,
    val risks: String?,
    val category: Category,
    val country: String,
    val location: Location?,
    val createdAt: Long,
    val launchedAt: Long,
    val deadlineAt: Long,
    val goal: Money,
    val pledged: Money,
    val usdExchangeRate: Double,
    val state: String,
    val backersCount: Int,
    val commentsCount: Int,
    val updatesCount: Int,
    val rewards: List<Reward>,
    val minPledge: Double?,
    val prelaunchActivated: Boolean,
    val isProjectWeLove: Boolean,
    val hasVideo: Boolean,
    val creator: Creator,
    val percentFunded: Double
)

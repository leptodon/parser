package domain.model

import java.time.Instant

data class Project(
    val id: String,
    val pid: Long?,
    val name: String,
    val slug: String,
    val description: String,
    val backerCount: Int,
    val goal: Money,
    val pledged: Money,
    val percentFunded: Int?,
    val creatorName: String,
    val creatorBackingsCount: Int?,
    val creatorLaunchedProjectsCount: Int?,
    val category: String,
    val subcategory: String?,
    val country: String,
    val currency: String?,
    val deadline: Instant?,
    val launchedAt: Instant?,
    val state: String,
    val isProjectWeLove: Boolean,
    val location: String?
)

data class Money(
    val amount: Double,
    val currency: String,
    val symbol: String
)
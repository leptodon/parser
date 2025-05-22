package data.repository.mapping

import data.api.model.ProjectDetailsResponse
import domain.model.Money
import domain.model.Project
import domain.model.ProjectDetails
import domain.model.Reward
import java.time.Instant
import java.time.LocalDate

class ProjectDetailsMapper {
    fun mapFromResponse(response: ProjectDetailsResponse): ProjectDetails {
        val projectData = response.data.project

        val project = Project(
            id = projectData.id,
            pid = null,
            name = projectData.name,
            slug = projectData.slug,
            description = projectData.description,
            backerCount = projectData.backersCount,
            goal = Money(
                amount = projectData.goal.amount.toDouble(),
                currency = projectData.goal.currency,
                symbol = projectData.goal.symbol
            ),
            pledged = Money(
                amount = projectData.pledged.amount.toDouble(),
                currency = projectData.pledged.currency,
                symbol = projectData.pledged.symbol
            ),
            percentFunded = null,
            creatorName = projectData.creator.name,
            creatorBackingsCount = projectData.creator.backingsCount,
            creatorLaunchedProjectsCount = projectData.creator.launchedProjects?.totalCount,
            category = projectData.category.parentCategory?.name ?: projectData.category.name,
            subcategory = if (projectData.category.parentCategory != null) projectData.category.name else null,
            country = projectData.country.name,
            currency = projectData.currency,
            deadline = Instant.ofEpochSecond(projectData.deadlineAt),
            launchedAt = projectData.launchedAt?.let { Instant.ofEpochSecond(it) },
            state = projectData.state,
            isProjectWeLove = projectData.isProjectWeLove,
            location = projectData.location?.displayableName
        )

        val rewards = projectData.rewards.nodes.map { rewardNode ->
            Reward(
                id = rewardNode.id,
                name = rewardNode.name,
                description = rewardNode.description,
                amount = Money(
                    amount = rewardNode.amount.amount.toDouble(),
                    currency = rewardNode.amount.currency,
                    symbol = rewardNode.amount.symbol
                ),
                backersCount = rewardNode.backersCount,
                estimatedDeliveryDate = rewardNode.estimatedDeliveryOn?.let {
                    LocalDate.parse(it)
                },
                isLimited = rewardNode.limit != null || rewardNode.remainingQuantity != null,
                remainingQuantity = rewardNode.remainingQuantity,
                limit = rewardNode.limit,
                hasShipping = rewardNode.shippingPreference != "none",
                shippingCountriesCount = rewardNode.simpleShippingRulesExpanded?.size ?: 0,
                isEarlyBird = rewardNode.name.contains("Early Bird", ignoreCase = true),
                startsAt = rewardNode.startsAt?.let { Instant.ofEpochSecond(it) },
                endsAt = rewardNode.endsAt?.let { Instant.ofEpochSecond(it) }
            )
        }

        val environmentalCommitments = projectData.environmentalCommitments?.map {
            "${it.commitmentCategory}: ${it.description}"
        } ?: emptyList()

        return ProjectDetails(
            project = project,
            story = projectData.story ?: "",
            risks = projectData.risks,
            rewards = rewards,
            environmentalCommitments = environmentalCommitments,
            faqCount = projectData.faqs?.nodes?.size ?: 0,
            commentsCount = projectData.commentsCount ?: 0,
            updatesCount = projectData.posts?.totalCount ?: 0,
            hasVideo = projectData.video?.videoSources?.high?.src != null
        )
    }
}
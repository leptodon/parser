package data.repository.mapping

import data.api.model.ProjectsResponse
import domain.model.Money
import domain.model.Project
import java.time.Instant

class ProjectMapper {
    fun mapFromResponse(response: ProjectsResponse): List<Project> {
        return response.data.projects.edges.map { edge ->
            val node = edge.node
            Project(
                id = node.id,
                pid = node.pid,
                name = node.name,
                slug = node.slug,
                description = node.description,
                backerCount = node.backersCount,
                goal = Money(
                    amount = node.goal.amount.toDouble(),
                    currency = node.goal.currency,
                    symbol = node.goal.symbol
                ),
                pledged = Money(
                    amount = node.pledged.amount.toDouble(),
                    currency = node.pledged.currency,
                    symbol = node.pledged.symbol
                ),
                percentFunded = node.percentFunded,
                creatorName = node.creator.name,
                creatorBackingsCount = null, // Will be populated in details
                creatorLaunchedProjectsCount = null, // Will be populated in details
                category = node.category.parentCategory?.name ?: node.category.name,
                subcategory = if (node.category.parentCategory != null) node.category.name else null,
                country = node.country.name,
                currency = node.goal.currency,
                deadline = Instant.ofEpochSecond(node.deadlineAt),
                launchedAt = node.launchedAt?.let { Instant.ofEpochSecond(it) },
                state = node.state,
                isProjectWeLove = node.isProjectWeLove,
                location = node.location?.displayableName
            )
        }
    }
}
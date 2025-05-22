package data.storage

import domain.model.ProjectDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CsvWriter {
    companion object {
        private const val PROJECTS_FILENAME = "kickstarter_projects.csv"
        private const val REWARDS_FILENAME = "kickstarter_rewards.csv"
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    }

    private val projectsFile: File
    private val rewardsFile: File

    init {
        val timestamp = LocalDateTime.now().format(FORMATTER)
        val outputDir = Paths.get("output", timestamp).toFile()
        outputDir.mkdirs()

        projectsFile = File(outputDir, PROJECTS_FILENAME)
        rewardsFile = File(outputDir, REWARDS_FILENAME)

        // Write headers
        projectsFile.writeText(getProjectsCsvHeader() + "\n")
        rewardsFile.writeText(getRewardsCsvHeader() + "\n")
    }

    private fun getProjectsCsvHeader(): String {
        return listOf(
            "id",
            "name",
            "slug",
            "description",
            "story",
            "risks",
            "backer_count",
            "goal_amount",
            "goal_currency",
            "pledged_amount",
            "pledged_currency",
            "percent_funded",
            "creator_name",
            "creator_backings_count",
            "creator_projects_count",
            "category",
            "subcategory",
            "country",
            "currency",
            "deadline",
            "launched_at",
            "state",
            "is_project_we_love",
            "location",
            "faq_count",
            "comments_count",
            "updates_count",
            "has_video",
            "environmental_commitments_count",
            "rewards_count",
            "min_pledge"
        ).joinToString(",")
    }

    private fun getRewardsCsvHeader(): String {
        return listOf(
            "project_id",
            "reward_id",
            "name",
            "description",
            "amount",
            "currency",
            "backers_count",
            "estimated_delivery_date",
            "is_limited",
            "remaining_quantity",
            "limit",
            "has_shipping",
            "shipping_countries_count",
            "is_early_bird",
            "starts_at",
            "ends_at"
        ).joinToString(",")
    }

    suspend fun writeProjectDetails(projectDetails: ProjectDetails) = withContext(Dispatchers.IO) {
        val project = projectDetails.project

        // Write project data
        val projectRow = listOf(
            escape(project.id),
            escape(project.name),
            escape(project.slug),
            escape(project.description),
            escape(projectDetails.story),
            escape(projectDetails.risks ?: ""),
            project.backerCount.toString(),
            project.goal.amount.toString(),
            project.goal.currency,
            project.pledged.amount.toString(),
            project.pledged.currency,
            project.percentFunded?.toString() ?: "",
            escape(project.creatorName),
            project.creatorBackingsCount?.toString() ?: "",
            project.creatorLaunchedProjectsCount?.toString() ?: "",
            escape(project.category),
            escape(project.subcategory ?: ""),
            project.country,
            project.currency,
            project.deadline.toString(),
            project.launchedAt?.toString() ?: "",
            project.state,
            project.isProjectWeLove.toString(),
            escape(project.location ?: ""),
            projectDetails.faqCount.toString(),
            projectDetails.commentsCount.toString(),
            projectDetails.updatesCount.toString(),
            projectDetails.hasVideo.toString(),
            projectDetails.environmentalCommitments.size.toString(),
            projectDetails.rewards.size.toString(),
            projectDetails.project.goal.amount.toString()
        ).joinToString(",")

        Files.write(
            projectsFile.toPath(),
            (projectRow + "\n").toByteArray(),
            StandardOpenOption.APPEND
        )

        // Write rewards data
        projectDetails.rewards.forEach { reward ->
            val rewardRow = listOf(
                escape(project.id),
                escape(reward.id),
                escape(reward.name),
                escape(reward.description),
                reward.amount.amount.toString(),
                reward.amount.currency,
                reward.backersCount.toString(),
                reward.estimatedDeliveryDate?.toString() ?: "",
                reward.isLimited.toString(),
                reward.remainingQuantity?.toString() ?: "",
                reward.limit?.toString() ?: "",
                reward.hasShipping.toString(),
                reward.shippingCountriesCount.toString(),
                reward.isEarlyBird.toString(),
                reward.startsAt?.toString() ?: "",
                reward.endsAt?.toString() ?: ""
            ).joinToString(",")

            Files.write(
                rewardsFile.toPath(),
                (rewardRow + "\n").toByteArray(),
                StandardOpenOption.APPEND
            )
        }
    }

    private fun escape(text: String): String {
        // Escape commas, quotes and newlines for CSV
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }
}
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

class BasicDataExporter {
    companion object {
        private const val PROJECTS_FILENAME = "kickstarter_projects_ml.json"
        private const val FEATURES_FILENAME = "kickstarter_features_ml.csv"
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    }

    private val outputDir: File
    private val projectsFile: File
    private val featuresFile: File

    init {
        val timestamp = LocalDateTime.now().format(FORMATTER)
        outputDir = Paths.get("output", timestamp).toFile()
        outputDir.mkdirs()

        projectsFile = File(outputDir, PROJECTS_FILENAME)
        featuresFile = File(outputDir, FEATURES_FILENAME)

        // Инициализируем файлы
        projectsFile.writeText("[\n")
        featuresFile.writeText(getFeaturesCsvHeader() + "\n")
    }

    private var isFirstProject = true

    suspend fun addProject(projectDetails: ProjectDetails) = withContext(Dispatchers.IO) {
        // Добавляем запятую перед проектом (кроме первого)
        if (!isFirstProject) {
            Files.write(
                projectsFile.toPath(),
                ",\n".toByteArray(),
                StandardOpenOption.APPEND
            )
        } else {
            isFirstProject = false
        }

        // Сохраняем JSON вручную (избегаем проблем с сериализацией)
        saveProjectAsJson(projectDetails)

        // Сохраняем признаки в CSV
        saveFeaturesToCsv(projectDetails)
    }

    private fun saveProjectAsJson(projectDetails: ProjectDetails) {
        val project = projectDetails.project

        val jsonProject = buildString {
            appendLine("  {")
            appendLine("    \"projectId\": \"${escapeJson(project.id)}\",")
            appendLine("    \"name\": \"${escapeJson(project.name)}\",")
            appendLine("    \"slug\": \"${escapeJson(project.slug)}\",")
            appendLine("    \"description\": \"${escapeJson(project.description.take(1000))}\",")
            appendLine("    \"story\": \"${escapeJson(projectDetails.story.take(1000))}\",")
            appendLine("    \"risks\": \"${escapeJson(projectDetails.risks?.take(500) ?: "")}\",")

            appendLine("    \"goalAmount\": ${project.goal.amount},")
            appendLine("    \"goalCurrency\": \"${project.goal.currency}\",")
            appendLine("    \"pledgedAmount\": ${project.pledged.amount},")
            appendLine("    \"pledgedCurrency\": \"${project.pledged.currency}\",")
            appendLine("    \"percentFunded\": ${project.percentFunded ?: 0},")
            appendLine("    \"backerCount\": ${project.backerCount},")

            appendLine("    \"creatorName\": \"${escapeJson(project.creatorName)}\",")
            appendLine("    \"creatorBackingsCount\": ${project.creatorBackingsCount ?: 0},")
            appendLine("    \"creatorProjectsCount\": ${project.creatorLaunchedProjectsCount ?: 0},")

            appendLine("    \"category\": \"${escapeJson(project.category)}\",")
            appendLine("    \"subcategory\": \"${escapeJson(project.subcategory ?: "")}\",")
            appendLine("    \"country\": \"${escapeJson(project.country)}\",")
            appendLine("    \"currency\": \"${escapeJson(project.currency)}\",")
            appendLine("    \"location\": \"${escapeJson(project.location ?: "")}\",")

            appendLine("    \"deadlineEpoch\": ${project.deadline.epochSecond},")
            appendLine("    \"launchedAtEpoch\": ${project.launchedAt?.epochSecond ?: 0},")

            appendLine("    \"state\": \"${escapeJson(project.state)}\",")
            appendLine("    \"isProjectWeLove\": ${project.isProjectWeLove},")
            appendLine("    \"isSuccessful\": ${project.state == "successful"},")

            appendLine("    \"hasVideo\": ${projectDetails.hasVideo},")
            appendLine("    \"faqCount\": ${projectDetails.faqCount},")
            appendLine("    \"commentsCount\": ${projectDetails.commentsCount},")
            appendLine("    \"updatesCount\": ${projectDetails.updatesCount},")
            appendLine("    \"environmentalCommitmentsCount\": ${projectDetails.environmentalCommitments.size},")

            // Рассчитанные признаки
            appendLine("    \"features\": {")
            appendLine("      \"descriptionLength\": ${project.description.length},")
            appendLine("      \"storyLength\": ${projectDetails.story.length},")
            appendLine("      \"hasRisks\": ${!projectDetails.risks.isNullOrEmpty()},")
            appendLine("      \"risksWordCount\": ${projectDetails.risks?.split("\\s+".toRegex())?.size ?: 0},")
            appendLine("      \"titleLength\": ${project.name.length},")
            appendLine("      \"titleWordCount\": ${project.name.split("\\s+".toRegex()).size},")
            appendLine("      \"descriptionWordCount\": ${project.description.split("\\s+".toRegex()).size},")

            appendLine("      \"rewardsCount\": ${projectDetails.rewards.size},")
            appendLine("      \"minRewardAmount\": ${projectDetails.rewards.minOfOrNull { it.amount.amount } ?: 0.0},")
            appendLine("      \"maxRewardAmount\": ${projectDetails.rewards.maxOfOrNull { it.amount.amount } ?: 0.0},")
            appendLine("      \"avgRewardAmount\": ${if (projectDetails.rewards.isEmpty()) 0.0 else projectDetails.rewards.map { it.amount.amount }.average()},")
            appendLine("      \"hasEarlyBirdRewards\": ${projectDetails.rewards.any { it.isEarlyBird }},")
            appendLine("      \"hasLimitedRewards\": ${projectDetails.rewards.any { it.isLimited }},")
            appendLine("      \"hasShippingRewards\": ${projectDetails.rewards.any { it.hasShipping }},")
            appendLine("      \"totalBackersFromRewards\": ${projectDetails.rewards.sumOf { it.backersCount }},")

            val durationDays = if (project.launchedAt != null) {
                ((project.deadline.epochSecond - project.launchedAt.epochSecond) / 86400).toInt()
            } else 0
            appendLine("      \"durationDays\": $durationDays,")

            appendLine("      \"fundingRatio\": ${project.pledged.amount / project.goal.amount},")
            appendLine("      \"avgPledgePerBacker\": ${if (project.backerCount > 0) project.pledged.amount / project.backerCount else 0.0}")
            appendLine("    },")

            // Упрощенные награды
            appendLine("    \"rewardsCount\": ${projectDetails.rewards.size},")
            appendLine("    \"environmentalCommitments\": [")
            projectDetails.environmentalCommitments.forEachIndexed { index, commitment ->
                val comma = if (index < projectDetails.environmentalCommitments.size - 1) "," else ""
                appendLine("      \"${escapeJson(commitment)}\"$comma")
            }
            appendLine("    ]")
            append("  }")
        }

        Files.write(
            projectsFile.toPath(),
            jsonProject.toByteArray(),
            StandardOpenOption.APPEND
        )
    }

    private fun saveFeaturesToCsv(projectDetails: ProjectDetails) {
        val project = projectDetails.project
        val features = listOf(
            // ID для связи
            escape(project.id),

            // Таргеты
            project.goal.amount.toString(),
            project.pledged.amount.toString(),
            project.percentFunded?.toString() ?: "0",
            project.backerCount.toString(),
            (project.state == "successful").toString(),

            // Числовые признаки
            project.description.length.toString(),
            projectDetails.story.length.toString(),
            (!projectDetails.risks.isNullOrEmpty()).toString(),
            (projectDetails.risks?.split("\\s+".toRegex())?.size ?: 0).toString(),
            projectDetails.hasVideo.toString(),
            projectDetails.faqCount.toString(),
            projectDetails.commentsCount.toString(),
            projectDetails.updatesCount.toString(),
            projectDetails.rewards.size.toString(),
            (projectDetails.rewards.minOfOrNull { it.amount.amount } ?: 0.0).toString(),
            (projectDetails.rewards.maxOfOrNull { it.amount.amount } ?: 0.0).toString(),
            (if (projectDetails.rewards.isEmpty()) 0.0 else projectDetails.rewards.map { it.amount.amount }.average()).toString(),
            projectDetails.rewards.any { it.isEarlyBird }.toString(),
            projectDetails.rewards.any { it.isLimited }.toString(),
            projectDetails.rewards.any { it.hasShipping }.toString(),

            // Категориальные признаки
            escape(project.category),
            escape(project.subcategory ?: ""),
            escape(project.country),
            escape(project.currency),

            // Временные признаки
            if (project.launchedAt != null) {
                ((project.deadline.epochSecond - project.launchedAt.epochSecond) / 86400).toString()
            } else "0",

            // Создатель
            escape(project.creatorName),
            project.creatorBackingsCount?.toString() ?: "0",
            project.creatorLaunchedProjectsCount?.toString() ?: "0",

            // Булевые признаки
            project.isProjectWeLove.toString(),
            (!project.location.isNullOrEmpty()).toString(),
            projectDetails.environmentalCommitments.size.toString(),

            // Текстовые метрики
            project.name.length.toString(),
            project.name.split("\\s+".toRegex()).size.toString(),
            project.description.split("\\s+".toRegex()).size.toString(),

            // Производные метрики
            projectDetails.rewards.sumOf { it.backersCount }.toString(),
            (project.pledged.amount / project.goal.amount).toString(),
            (if (project.backerCount > 0) project.pledged.amount / project.backerCount else 0.0).toString()
        ).joinToString(",")

        Files.write(
            featuresFile.toPath(),
            (features + "\n").toByteArray(),
            StandardOpenOption.APPEND
        )
    }

    private fun getFeaturesCsvHeader(): String {
        return listOf(
            "project_id",
            "goal_amount", "pledged_amount", "percent_funded", "backer_count", "is_successful",
            "description_length", "story_length", "has_risks", "risks_word_count",
            "has_video", "faq_count", "comments_count", "updates_count", "rewards_count",
            "min_reward_amount", "max_reward_amount", "avg_reward_amount",
            "has_early_bird_rewards", "has_limited_rewards", "has_shipping_rewards",
            "category", "subcategory", "country", "currency", "duration_days",
            "creator_name", "creator_backings_count", "creator_projects_count",
            "is_project_we_love", "has_location", "environmental_commitments_count",
            "title_length", "title_word_count", "description_word_count",
            "total_backers_from_rewards", "funding_ratio", "avg_pledge_per_backer"
        ).joinToString(",")
    }

    private fun escape(text: String): String {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }

    private fun escapeJson(text: String): String {
        return text.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun finalize() {
        // Закрываем JSON массив
        Files.write(
            projectsFile.toPath(),
            "\n]".toByteArray(),
            StandardOpenOption.APPEND
        )
    }

    fun getOutputDirectory(): String = outputDir.absolutePath
}
package data.storage

import domain.model.ProjectDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ImprovedDataExporter {
    companion object {
        private const val PROJECTS_FILENAME = "kickstarter_projects.json"
        private const val TRAINING_FILENAME = "kickstarter_training_data.json"
        private const val FEATURES_FILENAME = "kickstarter_features.csv"
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val outputDir: File
    private val projectsFile: File
    private val trainingFile: File
    private val featuresFile: File

    private val projects = mutableListOf<ProjectDetails>()

    init {
        val timestamp = LocalDateTime.now().format(FORMATTER)
        outputDir = Paths.get("output", timestamp).toFile()
        outputDir.mkdirs()

        projectsFile = File(outputDir, PROJECTS_FILENAME)
        trainingFile = File(outputDir, TRAINING_FILENAME)
        featuresFile = File(outputDir, FEATURES_FILENAME)

        // Инициализируем JSON файлы
        projectsFile.writeText("[]")
        trainingFile.writeText("[]")

        // Записываем заголовок для CSV с признаками
        featuresFile.writeText(getFeaturesCsvHeader() + "\n")
    }

    suspend fun addProject(projectDetails: ProjectDetails) = withContext(Dispatchers.IO) {
        projects.add(projectDetails)

        // Сохраняем полные данные в JSON
        saveFullProjectsData()

        // Сохраняем данные для обучения в JSON
        saveTrainingData()

        // Сохраняем признаки в CSV
        saveFeaturesToCsv(projectDetails)
    }

    private fun saveFullProjectsData() {
        val jsonData = json.encodeToString(projects)
        projectsFile.writeText(jsonData)
    }

    private fun saveTrainingData() {
        val trainingData = projects.map { project ->
            TrainingDataPoint(
                // Основные идентификаторы
                projectId = project.project.id,
                name = project.project.name,
                slug = project.project.slug,

                // Цели и результаты (таргеты для обучения)
                goalAmount = project.project.goal.amount,
                pledgedAmount = project.project.pledged.amount,
                percentFunded = project.project.percentFunded,
                backerCount = project.project.backerCount,
                isSuccessful = project.project.state == "successful",

                // Признаки проекта
                features = ProjectFeatures(
                    // Текстовые характеристики
                    descriptionLength = project.project.description.length,
                    storyLength = project.story.length,
                    hasRisks = !project.risks.isNullOrEmpty(),
                    risksWordCount = project.risks?.split("\\s+".toRegex())?.size ?: 0,

                    // Медиа контент
                    hasVideo = project.hasVideo,

                    // Активность и вовлеченность
                    faqCount = project.faqCount,
                    commentsCount = project.commentsCount,
                    updatesCount = project.updatesCount,

                    // Награды и предложения
                    rewardsCount = project.rewards.size,
                    minRewardAmount = project.rewards.minOfOrNull { it.amount.amount } ?: 0.0,
                    maxRewardAmount = project.rewards.maxOfOrNull { it.amount.amount } ?: 0.0,
                    avgRewardAmount = if (project.rewards.isEmpty()) 0.0 else project.rewards.map { it.amount.amount }.average(),
                    hasEarlyBirdRewards = project.rewards.any { it.isEarlyBird },
                    hasLimitedRewards = project.rewards.any { it.isLimited },
                    hasShippingRewards = project.rewards.any { it.hasShipping },

                    // Категория и географические данные
                    category = project.project.category,
                    subcategory = project.project.subcategory,
                    country = project.project.country,
                    currency = project.project.currency,

                    // Временные характеристики
                    durationDays = if (project.project.launchedAt != null) {
                        ((project.project.deadline.epochSecond - project.project.launchedAt.epochSecond) / 86400).toInt()
                    } else null,

                    // Создатель
                    creatorName = project.project.creatorName,
                    creatorBackingsCount = project.project.creatorBackingsCount,
                    creatorProjectsCount = project.project.creatorLaunchedProjectsCount,

                    // Дополнительные характеристики
                    isProjectWeLove = project.project.isProjectWeLove,
                    hasLocation = !project.project.location.isNullOrEmpty(),
                    environmentalCommitmentsCount = project.environmentalCommitments.size,

                    // Числовые характеристики текста
                    titleLength = project.project.name.length,
                    titleWordCount = project.project.name.split("\\s+".toRegex()).size,
                    descriptionWordCount = project.project.description.split("\\s+".toRegex()).size,

                    // Анализ наград
                    totalBackersFromRewards = project.rewards.sumOf { it.backersCount },
                    rewardTiers = project.rewards.map { it.amount.amount }.distinct().sorted(),

                    // Метрики успеха
                    fundingRatio = project.project.pledged.amount / project.project.goal.amount,
                    avgPledgePerBacker = if (project.project.backerCount > 0) {
                        project.project.pledged.amount / project.project.backerCount
                    } else 0.0
                )
            )
        }

        val jsonData = json.encodeToString(trainingData)
        trainingFile.writeText(jsonData)
    }

    private fun saveFeaturesToCsv(projectDetails: ProjectDetails) {
        val project = projectDetails.project
        val features = listOf(
            // ID для связи
            escape(project.id),

            // Таргеты
            project.goal.amount.toString(),
            project.pledged.amount.toString(),
            project.percentFunded?.toString() ?: "",
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
            } else "",

            // Создатель
            escape(project.creatorName),
            project.creatorBackingsCount?.toString() ?: "",
            project.creatorLaunchedProjectsCount?.toString() ?: "",

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

            // Таргеты
            "goal_amount",
            "pledged_amount",
            "percent_funded",
            "backer_count",
            "is_successful",

            // Числовые признаки
            "description_length",
            "story_length",
            "has_risks",
            "risks_word_count",
            "has_video",
            "faq_count",
            "comments_count",
            "updates_count",
            "rewards_count",
            "min_reward_amount",
            "max_reward_amount",
            "avg_reward_amount",
            "has_early_bird_rewards",
            "has_limited_rewards",
            "has_shipping_rewards",

            // Категориальные признаки
            "category",
            "subcategory",
            "country",
            "currency",

            // Временные признаки
            "duration_days",

            // Создатель
            "creator_name",
            "creator_backings_count",
            "creator_projects_count",

            // Булевые признаки
            "is_project_we_love",
            "has_location",
            "environmental_commitments_count",

            // Текстовые метрики
            "title_length",
            "title_word_count",
            "description_word_count",

            // Производные метрики
            "total_backers_from_rewards",
            "funding_ratio",
            "avg_pledge_per_backer"
        ).joinToString(",")
    }

    private fun escape(text: String): String {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }

    fun getOutputDirectory(): String = outputDir.absolutePath
}

// Модели данных для сериализации
@kotlinx.serialization.Serializable
data class TrainingDataPoint(
    val projectId: String,
    val name: String,
    val slug: String,
    val goalAmount: Double,
    val pledgedAmount: Double,
    val percentFunded: Int?,
    val backerCount: Int,
    val isSuccessful: Boolean,
    val features: ProjectFeatures
)

@kotlinx.serialization.Serializable
data class ProjectFeatures(
    // Текстовые характеристики
    val descriptionLength: Int,
    val storyLength: Int,
    val hasRisks: Boolean,
    val risksWordCount: Int,

    // Медиа контент
    val hasVideo: Boolean,

    // Активность и вовлеченность
    val faqCount: Int,
    val commentsCount: Int,
    val updatesCount: Int,

    // Награды и предложения
    val rewardsCount: Int,
    val minRewardAmount: Double,
    val maxRewardAmount: Double,
    val avgRewardAmount: Double,
    val hasEarlyBirdRewards: Boolean,
    val hasLimitedRewards: Boolean,
    val hasShippingRewards: Boolean,

    // Категория и географические данные
    val category: String,
    val subcategory: String?,
    val country: String,
    val currency: String,

    // Временные характеристики
    val durationDays: Int?,

    // Создатель
    val creatorName: String,
    val creatorBackingsCount: Int?,
    val creatorProjectsCount: Int?,

    // Дополнительные характеристики
    val isProjectWeLove: Boolean,
    val hasLocation: Boolean,
    val environmentalCommitmentsCount: Int,

    // Числовые характеристики текста
    val titleLength: Int,
    val titleWordCount: Int,
    val descriptionWordCount: Int,

    // Анализ наград
    val totalBackersFromRewards: Int,
    val rewardTiers: List<Double>,

    // Метрики успеха
    val fundingRatio: Double,
    val avgPledgePerBacker: Double
)
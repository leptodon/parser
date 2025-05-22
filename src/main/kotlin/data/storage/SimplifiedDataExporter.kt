package data.storage

import domain.model.ProjectDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SimplifiedDataExporter {
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

    private val projects = mutableListOf<ProjectDataSimple>()

    init {
        val timestamp = LocalDateTime.now().format(FORMATTER)
        outputDir = Paths.get("output", timestamp).toFile()
        outputDir.mkdirs()

        projectsFile = File(outputDir, PROJECTS_FILENAME)
        trainingFile = File(outputDir, TRAINING_FILENAME)
        featuresFile = File(outputDir, FEATURES_FILENAME)

        // Инициализируем файлы
        projectsFile.writeText("[]")
        trainingFile.writeText("[]")
        featuresFile.writeText(getFeaturesCsvHeader() + "\n")
    }

    suspend fun addProject(projectDetails: ProjectDetails) = withContext(Dispatchers.IO) {
        // Преобразуем в простые типы данных
        val ProjectDataSimple = convertToSimpleTypes(projectDetails)
        projects.add(ProjectDataSimple)

        // Сохраняем полные данные в JSON
        saveFullProjectsData()

        // Сохраняем данные для обучения в JSON
        saveTrainingData()

        // Сохраняем признаки в CSV
        saveFeaturesToCsv(projectDetails)
    }

    private fun convertToSimpleTypes(projectDetails: ProjectDetails): ProjectDataSimple {
        val project = projectDetails.project

        return ProjectDataSimple(
            // Основная информация
            projectId = project.id,
            name = project.name,
            slug = project.slug,
            description = project.description,
            story = projectDetails.story,
            risks = projectDetails.risks,

            // Финансовые данные
            goalAmount = project.goal.amount,
            goalCurrency = project.goal.currency,
            goalSymbol = project.goal.symbol,
            pledgedAmount = project.pledged.amount,
            pledgedCurrency = project.pledged.currency,
            pledgedSymbol = project.pledged.symbol,
            percentFunded = project.percentFunded,
            backerCount = project.backerCount,

            // Создатель
            creatorName = project.creatorName,
            creatorBackingsCount = project.creatorBackingsCount,
            creatorProjectsCount = project.creatorLaunchedProjectsCount,

            // Категории и локация
            category = project.category,
            subcategory = project.subcategory,
            country = project.country,
            currency = project.currency,
            location = project.location,

            // Временные данные (как строки)
            deadlineEpoch = project.deadline.epochSecond,
            launchedAtEpoch = project.launchedAt?.epochSecond,

            // Статус и флаги
            state = project.state,
            isProjectWeLove = project.isProjectWeLove,
            isSuccessful = project.state == "successful",

            // Контент и активность
            hasVideo = projectDetails.hasVideo,
            faqCount = projectDetails.faqCount,
            commentsCount = projectDetails.commentsCount,
            updatesCount = projectDetails.updatesCount,
            environmentalCommitmentsCount = projectDetails.environmentalCommitments.size,

            // Награды
            rewards = projectDetails.rewards.map { reward ->
                RewardDataSimple(
                    rewardId = reward.id,
                    name = reward.name,
                    description = reward.description,
                    amount = reward.amount.amount,
                    currency = reward.amount.currency,
                    symbol = reward.amount.symbol,
                    backersCount = reward.backersCount,
                    estimatedDeliveryDate = reward.estimatedDeliveryDate?.toString(),
                    isLimited = reward.isLimited,
                    remainingQuantity = reward.remainingQuantity,
                    limit = reward.limit,
                    hasShipping = reward.hasShipping,
                    shippingCountriesCount = reward.shippingCountriesCount,
                    isEarlyBird = reward.isEarlyBird,
                    startsAtEpoch = reward.startsAt?.epochSecond,
                    endsAtEpoch = reward.endsAt?.epochSecond
                )
            },

            // Дополнительные данные
            environmentalCommitments = projectDetails.environmentalCommitments,

            // Предвычисленные признаки для ML
            features = CalculatedFeaturesSimple(
                descriptionLength = project.description.length,
                storyLength = projectDetails.story.length,
                hasRisks = !projectDetails.risks.isNullOrEmpty(),
                risksWordCount = projectDetails.risks?.split("\\s+".toRegex())?.size ?: 0,
                titleLength = project.name.length,
                titleWordCount = project.name.split("\\s+".toRegex()).size,
                descriptionWordCount = project.description.split("\\s+".toRegex()).size,

                rewardsCount = projectDetails.rewards.size,
                minRewardAmount = projectDetails.rewards.minOfOrNull { it.amount.amount } ?: 0.0,
                maxRewardAmount = projectDetails.rewards.maxOfOrNull { it.amount.amount } ?: 0.0,
                avgRewardAmount = if (projectDetails.rewards.isEmpty()) 0.0 else projectDetails.rewards.map { it.amount.amount }.average(),
                hasEarlyBirdRewards = projectDetails.rewards.any { it.isEarlyBird },
                hasLimitedRewards = projectDetails.rewards.any { it.isLimited },
                hasShippingRewards = projectDetails.rewards.any { it.hasShipping },
                totalBackersFromRewards = projectDetails.rewards.sumOf { it.backersCount },

                durationDays = if (project.launchedAt != null) {
                    ((project.deadline.epochSecond - project.launchedAt.epochSecond) / 86400).toInt()
                } else null,

                fundingRatio = project.pledged.amount / project.goal.amount,
                avgPledgePerBacker = if (project.backerCount > 0) {
                    project.pledged.amount / project.backerCount
                } else 0.0
            )
        )
    }

    private fun saveFullProjectsData() {
        val jsonData = json.encodeToString(projects)
        projectsFile.writeText(jsonData)
    }

    private fun saveTrainingData() {
        val trainingData = projects.map { project ->
            TrainingDataPointSimple(
                projectId = project.projectId,
                name = project.name,
                slug = project.slug,

                // Таргеты
                goalAmount = project.goalAmount,
                pledgedAmount = project.pledgedAmount,
                percentFunded = project.percentFunded,
                backerCount = project.backerCount,
                isSuccessful = project.isSuccessful,

                // Признаки
                features = project.features
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

    fun getOutputDirectory(): String = outputDir.absolutePath
}

// Модели данных с простыми типами
@Serializable
data class ProjectDataSimple(
    val projectId: String,
    val name: String,
    val slug: String,
    val description: String,
    val story: String,
    val risks: String?,

    val goalAmount: Double,
    val goalCurrency: String,
    val goalSymbol: String,
    val pledgedAmount: Double,
    val pledgedCurrency: String,
    val pledgedSymbol: String,
    val percentFunded: Int?,
    val backerCount: Int,

    val creatorName: String,
    val creatorBackingsCount: Int?,
    val creatorProjectsCount: Int?,

    val category: String,
    val subcategory: String?,
    val country: String,
    val currency: String,
    val location: String?,

    val deadlineEpoch: Long,
    val launchedAtEpoch: Long?,

    val state: String,
    val isProjectWeLove: Boolean,
    val isSuccessful: Boolean,

    val hasVideo: Boolean,
    val faqCount: Int,
    val commentsCount: Int,
    val updatesCount: Int,
    val environmentalCommitmentsCount: Int,

    val rewards: List<RewardDataSimple>,
    val environmentalCommitments: List<String>,
    val features: CalculatedFeaturesSimple
)

@Serializable
data class RewardDataSimple(
    val rewardId: String,
    val name: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val symbol: String,
    val backersCount: Int,
    val estimatedDeliveryDate: String?,
    val isLimited: Boolean,
    val remainingQuantity: Int?,
    val limit: Int?,
    val hasShipping: Boolean,
    val shippingCountriesCount: Int,
    val isEarlyBird: Boolean,
    val startsAtEpoch: Long?,
    val endsAtEpoch: Long?
)

@Serializable
data class CalculatedFeaturesSimple(
    val descriptionLength: Int,
    val storyLength: Int,
    val hasRisks: Boolean,
    val risksWordCount: Int,
    val titleLength: Int,
    val titleWordCount: Int,
    val descriptionWordCount: Int,

    val rewardsCount: Int,
    val minRewardAmount: Double,
    val maxRewardAmount: Double,
    val avgRewardAmount: Double,
    val hasEarlyBirdRewards: Boolean,
    val hasLimitedRewards: Boolean,
    val hasShippingRewards: Boolean,
    val totalBackersFromRewards: Int,

    val durationDays: Int?,
    val fundingRatio: Double,
    val avgPledgePerBacker: Double
)

@Serializable
data class TrainingDataPointSimple(
    val projectId: String,
    val name: String,
    val slug: String,
    val goalAmount: Double,
    val pledgedAmount: Double,
    val percentFunded: Int?,
    val backerCount: Int,
    val isSuccessful: Boolean,
    val features: CalculatedFeaturesSimple
)
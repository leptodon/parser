package data.storage

import domain.model.ProjectDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.log10

class CsvWriter {
    companion object {
        private const val ML_DATASET_FILENAME = "kickstarter_ml_dataset.csv"
        private const val SESSION_FILE = "current_session.txt"
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        // Common words to filter out for readability score
        private val COMMON_WORDS = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "from", "up", "about", "into", "through", "during", "before", "after", "above", "below",
            "between", "among", "through", "during", "before", "after", "above", "below", "up", "down"
        )
    }

    private var outputDir: File
    private var mlDatasetFile: File

    init {
        // Попытаемся найти существующую сессию
        val sessionFile = File(SESSION_FILE)
        val existingSession = if (sessionFile.exists()) {
            sessionFile.readText().trim()
        } else null

        // Если есть существующая сессия и папка существует - используем её
        if (existingSession != null) {
            val existingDir = Paths.get("output", existingSession).toFile()
            if (existingDir.exists()) {
                outputDir = existingDir
                mlDatasetFile = File(outputDir, ML_DATASET_FILENAME)

                // Проверяем, что файл существует и не поврежден
                if (mlDatasetFile.exists() && mlDatasetFile.readLines().isNotEmpty()) {
                    println("📂 Resuming existing session: $existingSession")
                    println("📄 Continuing to write to: ${mlDatasetFile.absolutePath}")
                }
            }
        }

        // Создаем новую сессию
        val timestamp = LocalDateTime.now().format(FORMATTER)
        outputDir = Paths.get("output", timestamp).toFile()
        outputDir.mkdirs()

        mlDatasetFile = File(outputDir, ML_DATASET_FILENAME)

        // Записываем заголовок только для нового файла
        mlDatasetFile.writeText(getMlDatasetHeader() + "\n")

        // Сохраняем информацию о текущей сессии
        sessionFile.writeText(timestamp)

        println("📂 Started new session: $timestamp")
        println("📄 Created new dataset file: ${mlDatasetFile.absolutePath}")
    }

    private fun getMlDatasetHeader(): String {
        return listOf(
            // Target variables (3)
            "is_successful",
            "funding_ratio",
            "backer_count",

            // Text data - cleaned (4)
            "story",
            "description",
            "risks",
            "name",

            // Text metrics (9) - enhanced
            "story_length",
            "description_length",
            "title_length",
            "description_word_count",
            "title_word_count",
            "risks_word_count",
            "story_readability_score", // NEW
            "description_readability_score", // NEW
            "text_quality_score", // NEW

            // Structured features (20) - enhanced
            "goal_amount",
            "goal_amount_log", // NEW: log-transformed goal for better ML performance
            "funding_per_backer", // NEW: average pledge per backer
            "category",
            "subcategory",
            "country",
            "duration_days",
            "creator_projects_count",
            "creator_backings_count",
            "creator_experience_score", // NEW: combined creator experience
            "has_video",
            "rewards_count",
            "avg_reward_amount",
            "reward_price_range", // NEW: difference between max and min reward
            "has_early_bird_rewards",
            "has_limited_rewards",
            "has_risks",
            "faq_count",
            "updates_count",
            "is_project_we_love",
            "has_location"
        ).joinToString(",")
    }

    suspend fun writeProjectDetails(projectDetails: ProjectDetails) = withContext(Dispatchers.IO) {
        val project = projectDetails.project
        val rewards = projectDetails.rewards

        // Calculate derived metrics
        val isSuccessful = project.state == "successful"
        val fundingRatio = if (project.goal.amount > 0) {
            project.pledged.amount / project.goal.amount
        } else 0.0

        // Calculate duration in days
        val durationDays = if (project.launchedAt != null && project.deadline != null) {
            ChronoUnit.DAYS.between(project.launchedAt, project.deadline)
        } else 0L

        // Clean text data
        val cleanStory = cleanText(projectDetails.story)
        val cleanDescription = cleanText(project.description)
        val cleanRisks = cleanText(projectDetails.risks ?: "")
        val cleanName = cleanText(project.name)

        // Calculate basic text metrics
        val storyLength = cleanStory.length
        val descriptionLength = cleanDescription.length
        val titleLength = cleanName.length
        val descriptionWordCount = countWords(cleanDescription)
        val titleWordCount = countWords(cleanName)
        val risksWordCount = countWords(cleanRisks)

        // Calculate enhanced text metrics
        val storyReadabilityScore = calculateReadabilityScore(cleanStory)
        val descriptionReadabilityScore = calculateReadabilityScore(cleanDescription)
        val textQualityScore = calculateTextQualityScore(cleanStory, cleanDescription, cleanRisks)

        // Calculate enhanced numerical features
        val goalAmountLog = if (project.goal.amount > 0) log10(project.goal.amount) else 0.0
        val fundingPerBacker = if (project.backerCount > 0) {
            project.pledged.amount / project.backerCount
        } else 0.0

        // Calculate creator experience score
        val creatorExperienceScore = calculateCreatorExperienceScore(
            project.creatorLaunchedProjectsCount ?: 0,
            project.creatorBackingsCount ?: 0
        )

        // Calculate reward metrics
        val avgRewardAmount = if (rewards.isNotEmpty()) {
            rewards.map { it.amount.amount }.average()
        } else 0.0

        val rewardPriceRange = if (rewards.isNotEmpty()) {
            val sortedPrices = rewards.map { it.amount.amount }.sorted()
            sortedPrices.last() - sortedPrices.first()
        } else 0.0

        val hasEarlyBirdRewards = rewards.any { it.isEarlyBird }
        val hasLimitedRewards = rewards.any { it.isLimited }

        // Build the row
        val mlRow = listOf(
            // Target variables (3)
            isSuccessful.toString(),
            String.format("%.6f", fundingRatio),
            project.backerCount.toString(),

            // Text data - cleaned (4)
            escape(cleanStory),
            escape(cleanDescription),
            escape(cleanRisks),
            escape(cleanName),

            // Text metrics (9) - enhanced
            storyLength.toString(),
            descriptionLength.toString(),
            titleLength.toString(),
            descriptionWordCount.toString(),
            titleWordCount.toString(),
            risksWordCount.toString(),
            String.format("%.3f", storyReadabilityScore),
            String.format("%.3f", descriptionReadabilityScore),
            String.format("%.3f", textQualityScore),

            // Structured features (20) - enhanced
            project.goal.amount.toString(),
            String.format("%.6f", goalAmountLog),
            String.format("%.2f", fundingPerBacker),
            escape(project.category),
            escape(project.subcategory ?: ""),
            escape(project.country),
            durationDays.toString(),
            (project.creatorLaunchedProjectsCount ?: 0).toString(),
            (project.creatorBackingsCount ?: 0).toString(),
            String.format("%.3f", creatorExperienceScore),
            projectDetails.hasVideo.toString(),
            rewards.size.toString(),
            String.format("%.2f", avgRewardAmount),
            String.format("%.2f", rewardPriceRange),
            hasEarlyBirdRewards.toString(),
            hasLimitedRewards.toString(),
            (projectDetails.risks != null && projectDetails.risks.isNotBlank()).toString(),
            projectDetails.faqCount.toString(),
            projectDetails.updatesCount.toString(),
            project.isProjectWeLove.toString(),
            (project.location != null && project.location.isNotBlank()).toString()
        ).joinToString(",")

        // Ensure immediate write to disk after each project
        FileOutputStream(mlDatasetFile, true).use { fos ->
            fos.write((mlRow + "\n").toByteArray())
            fos.flush() // Force buffered data to be written
            fos.getFD().sync() // Force OS to write to physical disk
        }
    }

    // Метод для принудительного создания новой сессии
    fun startNewSession() {
        val sessionFile = File(SESSION_FILE)
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
        println("🔄 New session will be created on next startup")
    }

    // Метод для получения информации о текущей сессии
    fun getSessionInfo(): String {
        return if (mlDatasetFile.exists()) {
            val linesCount = mlDatasetFile.readLines().size - 1 // Минус заголовок
            "Current session: ${outputDir.name}, Projects saved: $linesCount, File: ${mlDatasetFile.absolutePath}"
        } else {
            "No active session"
        }
    }

    private fun cleanText(text: String): String {
        return text
            // Remove HTML tags
            .replace(Regex("<[^>]+>"), " ")
            // Remove URLs
            .replace(Regex("https?://\\S+"), " ")
            // Remove email addresses
            .replace(Regex("\\S+@\\S+\\.\\S+"), " ")
            // Replace multiple whitespace with single space
            .replace(Regex("\\s+"), " ")
            // Remove special characters except basic punctuation
            .replace(Regex("[^\\w\\s.,!?;:()\"'-]"), "")
            // Trim whitespace
            .trim()
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    private fun calculateReadabilityScore(text: String): Double {
        if (text.isBlank()) return 0.0

        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = text.split(Regex("[.!?]+")).filter { it.isNotBlank() }
        val syllables = words.sumOf { countSyllables(it) }

        if (sentences.isEmpty() || words.isEmpty()) return 0.0

        val avgWordsPerSentence = words.size.toDouble() / sentences.size
        val avgSyllablesPerWord = syllables.toDouble() / words.size

        // Simplified Flesch Reading Ease formula
        return maxOf(0.0, 206.835 - (1.015 * avgWordsPerSentence) - (84.6 * avgSyllablesPerWord))
    }

    private fun countSyllables(word: String): Int {
        val vowels = "aeiouyAEIOUY"
        var count = 0
        var prevWasVowel = false

        for (char in word) {
            val isVowel = char in vowels
            if (isVowel && !prevWasVowel) {
                count++
            }
            prevWasVowel = isVowel
        }

        // Silent 'e' rule
        if (word.endsWith("e", ignoreCase = true) && count > 1) {
            count--
        }

        return maxOf(1, count) // Every word has at least one syllable
    }

    private fun calculateTextQualityScore(story: String, description: String, risks: String): Double {
        val storyWords = countWords(story)
        val descWords = countWords(description)
        val risksWords = countWords(risks)

        // Score based on content completeness and balance
        var score = 0.0

        // Story completeness (0-4 points)
        score += when {
            storyWords > 500 -> 4.0
            storyWords > 200 -> 3.0
            storyWords > 100 -> 2.0
            storyWords > 50 -> 1.0
            else -> 0.0
        }

        // Description clarity (0-2 points)
        score += when {
            descWords > 20 -> 2.0
            descWords > 10 -> 1.0
            else -> 0.0
        }

        // Risks transparency (0-4 points)
        score += when {
            risksWords > 100 -> 4.0
            risksWords > 50 -> 3.0
            risksWords > 20 -> 2.0
            risksWords > 0 -> 1.0
            else -> 0.0
        }

        return score / 10.0 // Normalize to 0-1 scale
    }

    private fun calculateCreatorExperienceScore(projectsCount: Int, backingsCount: Int): Double {
        // Weighted score: launched projects matter more than backing others
        val projectScore = minOf(projectsCount * 0.7, 5.0) // Max 5 points from projects
        val backingScore = minOf(backingsCount * 0.1, 3.0) // Max 3 points from backings

        return (projectScore + backingScore) / 8.0 // Normalize to 0-1 scale
    }

    private fun escape(text: String): String {
        // Escape commas, quotes and newlines for CSV
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\""
        }
        return text
    }

    fun getOutputDirectory(): String = outputDir.absolutePath
}
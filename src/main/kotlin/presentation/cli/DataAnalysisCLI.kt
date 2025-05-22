package presentation.cli

import data.storage.SimplifiedDataExporter
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class DataAnalysisCLI(
    private val simplifiedDataExporter: SimplifiedDataExporter
) {
    private val logger = LoggerFactory.getLogger(DataAnalysisCLI::class.java)
    private val scanner = Scanner(System.`in`)

    fun showDataAnalysisMenu() {
        var running = true
        while (running) {
            println("\n=== Data Analysis Menu ===")
            println("1. Show data export formats")
            println("2. Analyze existing data")
            println("3. Export data statistics")
            println("4. Validate data quality")
            println("5. Generate ML feature summary")
            println("6. Back to main menu")
            print("\nEnter choice: ")

            when (scanner.nextLine().trim()) {
                "1" -> showDataFormats()
                "2" -> analyzeExistingData()
                "3" -> exportStatistics()
                "4" -> validateDataQuality()
                "5" -> generateFeatureSummary()
                "6" -> running = false
                else -> println("Invalid choice")
            }
        }
    }

    private fun showDataFormats() {
        println("\n=== Available Data Formats ===")
        println("1. kickstarter_projects.json - Полные данные проектов в JSON формате")
        println("   • Содержит все собранные данные о проектах")
        println("   • Подходит для детального анализа и исследований")
        println("   • Включает вложенные структуры (награды, характеристики)")

        println("\n2. kickstarter_training_data.json - Данные для обучения ML моделей")
        println("   • Структурированные данные с выделенными признаками")
        println("   • Таргеты для обучения: успешность, сумма сборов, количество бэкеров")
        println("   • Предобработанные числовые и категориальные признаки")

        println("\n3. kickstarter_features.csv - Признаки в табличном формате")
        println("   • Готовые признаки для ML алгоритмов")
        println("   • Численные и категориальные переменные")
        println("   • Подходит для быстрого анализа в Excel/Python/R")

        println("\n4. Устаревшие форматы (для совместимости):")
        println("   • kickstarter_projects.csv - Основные данные проектов")
        println("   • kickstarter_rewards.csv - Данные о наградах")

        println("\nРекомендуемые форматы для ML:")
        println("• Для глубокого обучения: kickstarter_training_data.json")
        println("• Для классических ML: kickstarter_features.csv")
        println("• Для исследовательского анализа: kickstarter_projects.json")
    }

    private fun analyzeExistingData() {
        val outputDir = simplifiedDataExporter.getOutputDirectory()
        println("\nAnalyzing data in: $outputDir")

        val trainingFile = File(outputDir, "kickstarter_training_data.json")
        val featuresFile = File(outputDir, "kickstarter_features.csv")

        if (trainingFile.exists()) {
            try {
                val content = trainingFile.readText()
                val lines = content.lines().filter { it.trim().isNotEmpty() }
                println("Training data file found: ${trainingFile.name}")
                println("File size: ${trainingFile.length()} bytes")
                println("Lines: ${lines.size}")

                // Простой анализ JSON структуры
                if (content.trim().startsWith("[")) {
                    val arraySize = content.count { it == '{' }
                    println("Estimated projects count: $arraySize")
                }
            } catch (e: Exception) {
                logger.error("Error analyzing training data", e)
            }
        }

        if (featuresFile.exists()) {
            try {
                val lines = featuresFile.readLines()
                println("\nFeatures CSV file found: ${featuresFile.name}")
                println("File size: ${featuresFile.length()} bytes")
                println("Total lines: ${lines.size}")
                if (lines.isNotEmpty()) {
                    println("Projects count: ${lines.size - 1}")
                    println("Features count: ${lines[0].split(",").size}")
                }
            } catch (e: Exception) {
                logger.error("Error analyzing features data", e)
            }
        }
    }

    private fun exportStatistics() {
        val outputDir = simplifiedDataExporter.getOutputDirectory()
        println("Exporting statistics to: $outputDir")

        // Здесь можно добавить логику для создания статистического отчета
        val statsFile = File(outputDir, "data_statistics.txt")

        try {
            val stats = buildString {
                appendLine("=== Kickstarter Data Statistics ===")
                appendLine("Generated at: ${System.currentTimeMillis()}")
                appendLine()

                // Анализ файлов
                val files = File(outputDir).listFiles()?.filter { it.isFile } ?: emptyList()
                appendLine("Files in output directory:")
                files.forEach { file ->
                    appendLine("  ${file.name}: ${file.length()} bytes")
                }

                appendLine()
                appendLine("=== Data Format Recommendations ===")
                appendLine("For Machine Learning:")
                appendLine("  • Use kickstarter_training_data.json for deep learning")
                appendLine("  • Use kickstarter_features.csv for traditional ML")
                appendLine("  • Features include: funding success, category, creator experience, etc.")
                appendLine()
                appendLine("For Analysis:")
                appendLine("  • Use kickstarter_projects.json for detailed exploration")
                appendLine("  • Contains complete project information and nested structures")
            }

            statsFile.writeText(stats)
            println("Statistics exported to: ${statsFile.name}")
        } catch (e: Exception) {
            logger.error("Error exporting statistics", e)
        }
    }

    private fun validateDataQuality() {
        val outputDir = simplifiedDataExporter.getOutputDirectory()
        println("Validating data quality in: $outputDir")

        val featuresFile = File(outputDir, "kickstarter_features.csv")

        if (!featuresFile.exists()) {
            println("No features file found for validation")
            return
        }

        try {
            val lines = featuresFile.readLines()
            if (lines.isEmpty()) {
                println("Features file is empty")
                return
            }

            val header = lines[0].split(",")
            val dataLines = lines.drop(1)

            println("Data Quality Report:")
            println("  Total records: ${dataLines.size}")
            println("  Features count: ${header.size}")

            // Проверка на пустые значения
            val emptyValuesCount = dataLines.sumOf { line ->
                line.split(",").count { field -> field.trim().isEmpty() }
            }
            println("  Empty values: $emptyValuesCount")

            // Проверка на дубликаты project_id
            val projectIds = dataLines.map { it.split(",")[0] }
            val uniqueIds = projectIds.toSet()
            if (projectIds.size != uniqueIds.size) {
                println("  WARNING: Duplicate project IDs found!")
            } else {
                println("  All project IDs are unique ✓")
            }

            println("  Data quality: ${if (emptyValuesCount < dataLines.size * 0.1) "Good" else "Needs attention"}")

        } catch (e: Exception) {
            logger.error("Error validating data quality", e)
        }
    }

    private fun generateFeatureSummary() {
        println("\n=== ML Feature Summary ===")
        println("Available features for machine learning:")

        val features = mapOf(
            "Target Variables" to listOf(
                "is_successful - Успешность проекта (булева)",
                "goal_amount - Целевая сумма сбора",
                "pledged_amount - Собранная сумма",
                "percent_funded - Процент финансирования",
                "backer_count - Количество бэкеров",
                "funding_ratio - Отношение собранного к цели"
            ),
            "Text Features" to listOf(
                "description_length - Длина описания проекта",
                "story_length - Длина истории проекта",
                "title_length - Длина названия",
                "title_word_count - Количество слов в названии",
                "description_word_count - Количество слов в описании",
                "risks_word_count - Количество слов в рисках"
            ),
            "Content Features" to listOf(
                "has_video - Наличие видео (булева)",
                "has_risks - Наличие описания рисков (булева)",
                "faq_count - Количество FAQ",
                "comments_count - Количество комментариев",
                "updates_count - Количество обновлений"
            ),
            "Reward Features" to listOf(
                "rewards_count - Количество наград",
                "min_reward_amount - Минимальная сумма награды",
                "max_reward_amount - Максимальная сумма награды",
                "avg_reward_amount - Средняя сумма награды",
                "has_early_bird_rewards - Наличие early bird наград",
                "has_limited_rewards - Наличие лимитированных наград",
                "has_shipping_rewards - Наличие наград с доставкой",
                "total_backers_from_rewards - Общее количество бэкеров наград"
            ),
            "Creator Features" to listOf(
                "creator_backings_count - Количество поддержанных проектов создателем",
                "creator_projects_count - Количество запущенных проектов создателем"
            ),
            "Categorical Features" to listOf(
                "category - Основная категория проекта",
                "subcategory - Подкатегория проекта",
                "country - Страна проекта",
                "currency - Валюта проекта"
            ),
            "Time Features" to listOf(
                "duration_days - Длительность кампании в днях"
            ),
            "Boolean Features" to listOf(
                "is_project_we_love - Отмечен как 'Projects We Love'",
                "has_location - Наличие указанной локации"
            ),
            "Derived Features" to listOf(
                "avg_pledge_per_backer - Средний взнос на бэкера",
                "environmental_commitments_count - Количество экологических обязательств"
            )
        )

        features.forEach { (category, featureList) ->
            println("\n$category:")
            featureList.forEach { feature ->
                println("  • $feature")
            }
        }

        println("\n=== Рекомендации по использованию ===")
        println("1. Для предсказания успешности проекта:")
        println("   • Используйте 'is_successful' как целевую переменную")
        println("   • Основные признаки: goal_amount, duration_days, category, creator_projects_count")

        println("\n2. Для предсказания суммы сборов:")
        println("   • Используйте 'pledged_amount' или 'funding_ratio' как целевую переменную")
        println("   • Основные признаки: description_length, rewards_count, has_video, category")

        println("\n3. Для предсказания количества бэкеров:")
        println("   • Используйте 'backer_count' как целевую переменную")
        println("   • Основные признаки: avg_reward_amount, has_early_bird_rewards, story_length")

        println("\n4. Предобработка данных:")
        println("   • Категориальные признаки требуют One-Hot Encoding")
        println("   • Текстовые признаки уже преобразованы в численные")
        println("   • Проверьте на наличие выбросов в суммах и количествах")

        // Сохраняем summary в файл
        val outputDir = simplifiedDataExporter.getOutputDirectory()
        val summaryFile = File(outputDir, "ml_features_summary.txt")

        try {
            val summaryText = buildString {
                appendLine("=== Machine Learning Features Summary ===")
                appendLine("Generated: ${java.time.LocalDateTime.now()}")
                appendLine()

                features.forEach { (category, featureList) ->
                    appendLine("$category:")
                    featureList.forEach { feature ->
                        appendLine("  • $feature")
                    }
                    appendLine()
                }

                appendLine("=== Usage Recommendations ===")
                appendLine("For project success prediction:")
                appendLine("  Target: is_successful")
                appendLine("  Key features: goal_amount, duration_days, category, creator_projects_count")
                appendLine()
                appendLine("For funding amount prediction:")
                appendLine("  Target: pledged_amount or funding_ratio")
                appendLine("  Key features: description_length, rewards_count, has_video, category")
                appendLine()
                appendLine("For backer count prediction:")
                appendLine("  Target: backer_count")
                appendLine("  Key features: avg_reward_amount, has_early_bird_rewards, story_length")
                appendLine()
                appendLine("Data preprocessing notes:")
                appendLine("  • Categorical features need One-Hot Encoding")
                appendLine("  • Text features are already converted to numerical")
                appendLine("  • Check for outliers in amounts and counts")
            }

            summaryFile.writeText(summaryText)
            println("\nFeature summary saved to: ${summaryFile.name}")
        } catch (e: Exception) {
            logger.error("Error saving feature summary", e)
        }
    }
}
package presentation.cli

import data.storage.CsvWriter
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent
import org.slf4j.LoggerFactory
import presentation.ParserController
import java.util.Scanner

class CommandLineInterface(
    private val parserController: ParserController
) {
    private val logger = LoggerFactory.getLogger(CommandLineInterface::class.java)
    private val scanner = Scanner(System.`in`)

    fun start() {
        println("=== Kickstarter Project Parser ===")
        println("ML-ready dataset export to CSV format")

        // Показываем информацию о текущей сессии
        showSessionInfo()

        var running = true
        while (running) {
            println("\n=== Main Menu ===")
            println("1. Start parsing projects")
            println("2. Stop parsing")
            println("3. Reset pagination")
            println("4. Show session info")
            println("5. Start new session")
            println("6. Show output format info")
            println("7. Exit")
            print("\nEnter command: ")

            when (scanner.nextLine().trim()) {
                "1" -> startParsing()
                "2" -> parserController.stopParsing()
                "3" -> parserController.resetPagination()
                "4" -> showSessionInfo()
                "5" -> startNewSession()
                "6" -> showOutputFormatInfo()
                "7" -> running = false
                else -> println("Invalid command")
            }
        }

        println("Exiting...")
    }

    private fun showSessionInfo() {
        try {
            val csvWriter = KoinJavaComponent.get<CsvWriter>(CsvWriter::class.java)
            println("\n📊 " + csvWriter.getSessionInfo())
        } catch (e: Exception) {
            println("⚠️ Could not get session info: ${e.message}")
        }
    }

    private fun startNewSession() {
        print("Are you sure you want to start a new session? This will create a new dataset file. (y/N): ")
        val confirmation = scanner.nextLine().trim().lowercase()

        if (confirmation == "y" || confirmation == "yes") {
            try {
                val csvWriter = KoinJavaComponent.get<CsvWriter>(CsvWriter::class.java)
                csvWriter.startNewSession()
                println("✅ New session will be created when you restart the parser.")
                println("💡 Please restart the application to begin the new session.")
            } catch (e: Exception) {
                println("❌ Error starting new session: ${e.message}")
            }
        } else {
            println("Cancelled.")
        }
    }

    private fun startParsing() {
        print("Enter batch size (default: 15): ")
        val batchSizeStr = scanner.nextLine().trim()
        val batchSize = if (batchSizeStr.isEmpty()) 15 else batchSizeStr.toIntOrNull() ?: 15

        print("Enter maximum projects to parse (0 for unlimited, default: 100): ")
        val maxProjectsStr = scanner.nextLine().trim()
        val maxProjects = if (maxProjectsStr.isEmpty()) 100 else maxProjectsStr.toIntOrNull() ?: 100

        print("Enter delay between requests in ms (default: 1000): ")
        val delayStr = scanner.nextLine().trim()
        val delay = if (delayStr.isEmpty()) 1000L else delayStr.toLongOrNull() ?: 1000L

        println("Starting parser with batch size: $batchSize, max projects: $maxProjects, delay: $delay ms")
        println("Data will be saved to: kickstarter_ml_dataset.csv")
        println("Each project is saved immediately to disk!")

        showSessionInfo()

        runBlocking {
            parserController.startParsing(
                batchSize = batchSize,
                maxProjects = maxProjects,
                delayBetweenRequests = delay,
                tokenProvider = {
                    println("\nAuthentication required. Please enter new authorization token:")
                    print("Token: ")
                    val token = scanner.nextLine().trim()
                    if (token.isEmpty()) null else token
                }
            )
        }
    }

    private fun showOutputFormatInfo() {
        println("\n=== Output Format Information ===")
        println()
        println("📊 File: kickstarter_ml_dataset.csv")
        println("📁 Location: output/[session-timestamp]/")
        println("💾 Write mode: Immediate (each project saved instantly)")
        println("🔄 Session management: Continues existing session on restart")
        println()
        println("📋 Session Management:")
        println("  • When you restart the parser, it continues the same session")
        println("  • New data is appended to the existing CSV file")
        println("  • Use 'Reset pagination' to start from beginning of Kickstarter data")
        println("  • Use 'Start new session' to create a completely new dataset file")
        println()
        println("🎯 Dataset Features (42 columns):")
        println()
        println("Target Variables (3):")
        println("  • is_successful - Project success (true/false)")
        println("  • funding_ratio - Pledged/Goal ratio")
        println("  • backer_count - Number of backers")
        println()
        println("Text Content (4):")
        println("  • story - Cleaned project story")
        println("  • description - Cleaned description")
        println("  • risks - Cleaned risks section")
        println("  • name - Cleaned project name")
        println()
        println("Text Metrics (9):")
        println("  • story_length, description_length, title_length")
        println("  • description_word_count, title_word_count, risks_word_count")
        println("  • story_readability_score, description_readability_score")
        println("  • text_quality_score")
        println()
        println("Project Features (20):")
        println("  • goal_amount, goal_amount_log")
        println("  • category, subcategory, country")
        println("  • duration_days")
        println("  • creator_projects_count, creator_backings_count, creator_experience_score")
        println("  • has_video, rewards_count, avg_reward_amount, reward_price_range")
        println("  • has_early_bird_rewards, has_limited_rewards, has_risks")
        println("  • faq_count, updates_count")
        println("  • is_project_we_love, has_location")
        println("  • funding_per_backer")
        println()
        println("✨ Enhanced Features:")
        println("  • Log-transformed goal amount for better ML performance")
        println("  • Readability scores using Flesch formula")
        println("  • Text quality score (0-1 scale)")
        println("  • Creator experience score (weighted combination)")
        println("  • Reward price range analysis")
        println()
        println("🎯 ML Use Cases:")
        println("  • Binary classification: Predict project success")
        println("  • Regression: Predict funding amount or backer count")
        println("  • Multi-class: Predict success level categories")
        println("  • Text analysis: Story/description impact on success")
        println()
        println("📋 Data Quality:")
        println("  • HTML tags removed from text fields")
        println("  • URLs and emails sanitized")
        println("  • CSV properly escaped (commas, quotes handled)")
        println("  • Missing values handled gracefully")
        println("  • Immediate disk writes prevent data loss")
        println("  • No duplicate data on restart (session continuation)")
    }
}
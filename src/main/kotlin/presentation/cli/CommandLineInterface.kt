package presentation.cli

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import presentation.ParserController
import java.util.Scanner

class CommandLineInterface(
    private val parserController: ParserController,
    private val dataAnalysisCLI: DataAnalysisCLI
) {
    private val logger = LoggerFactory.getLogger(CommandLineInterface::class.java)
    private val scanner = Scanner(System.`in`)

    fun start() {
        println("=== Kickstarter Project Parser & Data Analyzer ===")
        println("Enhanced version with ML-ready data export")

        var running = true
        while (running) {
            println("\n=== Main Menu ===")
            println("1. Start parsing projects")
            println("2. Stop parsing")
            println("3. Reset pagination")
            println("4. Data analysis & export options")
            println("5. Show data formats info")
            println("6. Exit")
            print("\nEnter command: ")

            when (scanner.nextLine().trim()) {
                "1" -> startParsing()
                "2" -> parserController.stopParsing()
                "3" -> parserController.resetPagination()
                "4" -> dataAnalysisCLI.showDataAnalysisMenu()
                "5" -> showDataFormatsInfo()
                "6" -> running = false
                else -> println("Invalid command")
            }
        }

        println("Exiting...")
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
        println("Data will be exported in multiple formats for different use cases:")
        println("  • JSON format for detailed analysis")
        println("  • ML-ready JSON for training models")
        println("  • CSV format for quick analysis")

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

    private fun showDataFormatsInfo() {
        println("\n=== Improved Data Export Formats ===")
        println()
        println("🎯 ML-Ready Formats (Recommended):")
        println("  📊 kickstarter_training_data.json")
        println("     • Structured data with extracted features")
        println("     • Ready for deep learning frameworks")
        println("     • Includes target variables for supervised learning")
        println()
        println("  📈 kickstarter_features.csv")
        println("     • Tabular format with numerical features")
        println("     • Perfect for scikit-learn, pandas analysis")
        println("     • Easy to import into Excel, R, or Python")
        println()
        println("🔍 Research Formats:")
        println("  📋 kickstarter_projects.json")
        println("     • Complete project data with all details")
        println("     • Hierarchical JSON structure")
        println("     • Best for exploratory data analysis")
        println()
        println("🔄 Legacy Formats (for compatibility):")
        println("  📄 kickstarter_projects.csv & kickstarter_rewards.csv")
        println("     • Original two-file format")
        println("     • Maintained for backward compatibility")
        println()
        println("💡 What's improved:")
        println("  ✅ Single comprehensive dataset vs. split files")
        println("  ✅ Pre-calculated ML features (text length, ratios, etc.)")
        println("  ✅ Proper target variables for different prediction tasks")
        println("  ✅ Categorical encoding preparation")
        println("  ✅ Feature engineering (funding ratio, avg pledge, etc.)")
        println()
        println("🎯 Use Cases:")
        println("  • Predict project success: Use 'is_successful' target")
        println("  • Predict funding amount: Use 'pledged_amount' target")
        println("  • Predict backer count: Use 'backer_count' target")
        println("  • Category analysis: Use 'category' and related features")
        println("  • Creator analysis: Use creator-related features")
    }
}
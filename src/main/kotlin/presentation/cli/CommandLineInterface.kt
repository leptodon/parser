package presentation.cli

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import presentation.ParserController
import java.util.Scanner

class CommandLineInterface(private val parserController: ParserController) {
    private val logger = LoggerFactory.getLogger(CommandLineInterface::class.java)
    private val scanner = Scanner(System.`in`)

    fun start() {
        println("=== Kickstarter Project Parser ===")

        var running = true
        while (running) {
            println("\nCommands:")
            println("1. Start parsing")
            println("2. Stop parsing")
            println("3. Reset pagination")
            println("4. Exit")
            print("\nEnter command: ")

            when (scanner.nextLine().trim()) {
                "1" -> startParsing()
                "2" -> parserController.stopParsing()
                "3" -> parserController.resetPagination()
                "4" -> running = false
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

        runBlocking {
            parserController.startParsing(
                batchSize = batchSize,
                maxProjects = maxProjects,
                delayBetweenRequests = delay,
                tokenProvider = {
                    println("\nAuthentication required. Please enter new authorization token:")
                    val token = scanner.nextLine().trim()
                    if (token.isEmpty()) null else token
                }
            )
        }
    }
}
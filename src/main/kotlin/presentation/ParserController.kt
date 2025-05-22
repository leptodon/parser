package presentation

import data.api.AuthException
import data.api.RateLimitException
import domain.model.Project
import domain.usecase.GetProjectDetailsUseCase
import domain.usecase.GetProjectsUseCase
import domain.usecase.SaveProjectsDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.fold
import kotlin.math.pow

class ParserController(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getProjectDetailsUseCase: GetProjectDetailsUseCase,
    private val saveProjectsDataUseCase: SaveProjectsDataUseCase
) {
    private val logger = LoggerFactory.getLogger(ParserController::class.java)
    private var isRunning = false
    private var requestNewToken: suspend () -> String? = { null }

    // Rate limiting state
    private var consecutiveRateLimitErrors = 0
    private var lastRateLimitTime = 0L

    suspend fun startParsing(
        batchSize: Int,
        maxProjects: Int,
        delayBetweenRequests: Long = 1000,
        tokenProvider: suspend () -> String?
    ) = withContext(Dispatchers.Default) {
        if (isRunning) {
            logger.warn("Parser is already running")
            return@withContext
        }

        isRunning = true
        requestNewToken = tokenProvider
        var processedCount = 0

        // Reset rate limiting state
        consecutiveRateLimitErrors = 0
        lastRateLimitTime = 0L

        try {
            while (getProjectsUseCase.hasMoreProjects() && (maxProjects <= 0 || processedCount < maxProjects)) {
                if (!isRunning) break

                val remainingCount = if (maxProjects <= 0) batchSize else minOf(batchSize, maxProjects - processedCount)
                if (remainingCount <= 0) break

                logger.info("Fetching batch of $remainingCount projects (processed: $processedCount)")

                val projectsResult = getProjectsUseCase.invoke(remainingCount)

                projectsResult.fold(
                    onSuccess = { projects ->
                        logger.info("Fetched ${projects.size} projects")

                        // Reset rate limit counters on successful request
                        consecutiveRateLimitErrors = 0

                        for (project in projects) {
                            if (!isRunning) break

                            try {
                                processProject(project)
                                processedCount++

                                // Log progress after each project is saved
                                logger.info("✅ Successfully processed and saved project #$processedCount: ${project.name}")

                                if (maxProjects > 0 && processedCount >= maxProjects) {
                                    logger.info("Reached maximum number of projects: $maxProjects")
                                    break
                                }

                                // Apply dynamic throttling based on recent rate limits
                                val dynamicDelay = calculateDynamicDelay(delayBetweenRequests)
                                if (dynamicDelay > delayBetweenRequests) {
                                    logger.info("⏳ Applying extended delay: ${dynamicDelay}ms (recent rate limits detected)")
                                }
                                delay(dynamicDelay)

                            } catch (e: AuthException) {
                                handleAuthError()
                            } catch (e: RateLimitException) {
                                handleRateLimitError()
                            } catch (e: Exception) {
                                logger.error("❌ Error processing project ${project.slug}", e)
                                // Continue with next project after a short delay
                                delay(2000)
                            }
                        }
                    },
                    onFailure = { error ->
                        when (error) {
                            is AuthException -> {
                                handleAuthError()
                            }
                            is RateLimitException -> {
                                handleRateLimitError()
                            }
                            else -> {
                                logger.error("Failed to fetch projects", error)
                                delay(5000) // Wait before retrying
                            }
                        }
                    }
                )
            }

            logger.info("🎉 Parsing completed. Processed $processedCount projects.")
        } finally {
            isRunning = false
        }
    }

    private suspend fun processProject(project: Project) {
        logger.info("🔄 Processing project: ${project.name} (${project.slug})")

        val detailsResult = getProjectDetailsUseCase(project.slug)

        detailsResult.fold(
            onSuccess = { projectDetails ->
                logger.info("📥 Fetched details for ${project.name}")

                saveProjectsDataUseCase(projectDetails).fold(
                    onSuccess = {
                        logger.info("💾 Data saved to CSV for ${project.name}")
                    },
                    onFailure = { error ->
                        logger.error("💥 Failed to save project data for ${project.name}", error)
                        throw error // Re-throw to stop processing this project
                    }
                )
            },
            onFailure = { error ->
                when (error) {
                    is AuthException -> {
                        throw error // Let the caller handle auth errors
                    }
                    is RateLimitException -> {
                        throw error // Let the caller handle rate limit errors
                    }
                    else -> {
                        logger.error("📱 Failed to fetch details for ${project.name}", error)
                        throw error // Re-throw to stop processing this project
                    }
                }
            }
        )
    }

    private suspend fun handleAuthError() {
        logger.warn("🚨 Authentication error. Requesting new token...")
        val newToken = requestNewToken()

        if (newToken == null) {
            logger.error("🔒 Failed to get new token. Stopping parser.")
            isRunning = false
        } else {
            logger.info("🔑 Received new token. Continuing...")
        }
    }

    private suspend fun handleRateLimitError() {
        consecutiveRateLimitErrors++
        val currentTime = System.currentTimeMillis()
        lastRateLimitTime = currentTime

        // Calculate exponential backoff with jitter
        val baseDelay = 60_000L // 1 minute base
        val backoffDelay = (baseDelay * (2.0.pow(consecutiveRateLimitErrors - 1))).toLong()
        val maxDelay = 600_000L // 10 minutes max
        val actualDelay = minOf(backoffDelay, maxDelay)

        // Add some jitter (±10%)
        val jitter = (actualDelay * 0.1 * (Math.random() - 0.5)).toLong()
        val finalDelay = actualDelay + jitter

        logger.warn("🚦 Rate limit exceeded (attempt #$consecutiveRateLimitErrors). Cooling down for ${finalDelay / 1000} seconds...")
        logger.info("💡 Tip: You can stop the parser and resume later if needed")

        // Show countdown
        var remainingSeconds = (finalDelay / 1000).toInt()
        while (remainingSeconds > 0 && isRunning) {
            if (remainingSeconds % 30 == 0 || remainingSeconds <= 10) {
                logger.info("⏰ Resuming in $remainingSeconds seconds...")
            }
            delay(1000)
            remainingSeconds--
        }

        if (isRunning) {
            logger.info("🟢 Cooldown complete. Resuming parsing...")
        }
    }

    private fun calculateDynamicDelay(baseDelay: Long): Long {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRateLimit = currentTime - lastRateLimitTime

        return when {
            // If we had rate limits recently (within last 5 minutes), be more conservative
            timeSinceLastRateLimit < 300_000L && consecutiveRateLimitErrors > 0 -> {
                baseDelay * 3 // Triple the delay
            }
            // If we had rate limits within last 10 minutes, be moderately conservative
            timeSinceLastRateLimit < 600_000L && consecutiveRateLimitErrors > 0 -> {
                baseDelay * 2 // Double the delay
            }
            // Normal operation
            else -> baseDelay
        }
    }

    fun stopParsing() {
        logger.info("⏹️ Stopping parser...")
        isRunning = false
    }

    fun resetPagination() {
        runBlocking {
            getProjectsUseCase.resetPagination()
            logger.info("🔄 Pagination reset. Will start from the beginning.")
        }
    }
}
package presentation

import data.api.AuthException
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

class ParserController(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getProjectDetailsUseCase: GetProjectDetailsUseCase,
    private val saveProjectsDataUseCase: SaveProjectsDataUseCase
) {
    private val logger = LoggerFactory.getLogger(ParserController::class.java)
    private var isRunning = false
    private var requestNewToken: suspend () -> String? = { null }

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

        try {
            while (getProjectsUseCase.hasMoreProjects() && (maxProjects <= 0 || processedCount < maxProjects)) {
                val remainingCount = if (maxProjects <= 0) batchSize else minOf(batchSize, maxProjects - processedCount)
                if (remainingCount <= 0) break

                logger.info("Fetching batch of $remainingCount projects (processed: $processedCount)")

                val projectsResult = getProjectsUseCase.invoke(remainingCount)

                projectsResult.fold(
                    onSuccess = { projects ->
                        logger.info("Fetched ${projects.size} projects")

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

                                // Throttle requests
                                delay(delayBetweenRequests)
                            } catch (e: AuthException) {
                                handleAuthError()
                            } catch (e: Exception) {
                                logger.error("❌ Error processing project ${project.slug}", e)
                                // Continue with next project
                            }
                        }
                    },
                    onFailure = { error ->
                        if (error is AuthException) {
                            handleAuthError()
                        } else {
                            logger.error("Failed to fetch projects", error)
                            delay(5000) // Wait before retrying
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
                if (error is AuthException) {
                    throw error // Let the caller handle auth errors
                } else {
                    logger.error("📱 Failed to fetch details for ${project.name}", error)
                    throw error // Re-throw to stop processing this project
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
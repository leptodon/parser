package data.repository

import data.api.AuthException
import data.api.KickstarterApi
import data.api.RateLimitException
import data.repository.mapping.ProjectDetailsMapper
import data.repository.mapping.ProjectMapper
import data.storage.StateManager
import domain.model.Project
import domain.model.ProjectDetails
import domain.repository.ProjectRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.math.pow
import kotlin.math.min

class ProjectRepositoryImpl(
    private val api: KickstarterApi,
    private val stateManager: StateManager,
    private val projectMapper: ProjectMapper,
    private val projectDetailsMapper: ProjectDetailsMapper
) : ProjectRepository {

    private val logger = LoggerFactory.getLogger(ProjectRepositoryImpl::class.java)

    // Rate limiting state
    private var consecutiveRateLimitErrors = 0
    private var lastRateLimitTime = 0L

    override suspend fun getProjects(limit: Int): Result<List<Project>> {
        val cursor = stateManager.getLastCursor()
        return try {
            val response = api.getProjects(cursor, limit)
            response.fold(
                onSuccess = { projectsResponse ->
                    // Reset rate limit counters on success
                    consecutiveRateLimitErrors = 0

                    val projects = projectMapper.mapFromResponse(projectsResponse)
                    val lastCursor = projectsResponse.data.projects.pageInfo.endCursor
                    stateManager.saveLastCursor(lastCursor)

                    Result.success(projects)
                },
                onFailure = { error ->
                    when (error) {
                        is AuthException -> {
                            logger.warn("Authentication error in getProjects")
                            Result.failure(error)
                        }
                        is RateLimitException -> {
                            logger.warn("Rate limit exceeded in getProjects - applying exponential backoff")
                            applyRateLimitBackoff()
                            Result.failure(error)
                        }
                        else -> {
                            logger.error("Generic error in getProjects, retrying with backoff", error)
                            delay(5000)
                            Result.failure(error)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectDetails(slug: String): Result<ProjectDetails> {
        return try {
            val response = api.getProjectDetails(slug)
            response.fold(
                onSuccess = { detailsResponse ->
                    // Reset rate limit counters on success
                    consecutiveRateLimitErrors = 0

                    val projectDetails = projectDetailsMapper.mapFromResponse(detailsResponse)
                    Result.success(projectDetails)
                },
                onFailure = { error ->
                    when (error) {
                        is AuthException -> {
                            logger.warn("Authentication error in getProjectDetails for $slug")
                            Result.failure(error)
                        }
                        is RateLimitException -> {
                            logger.warn("Rate limit exceeded in getProjectDetails for $slug - applying exponential backoff")
                            applyRateLimitBackoff()
                            Result.failure(error)
                        }
                        else -> {
                            logger.error("Generic error in getProjectDetails for $slug, retrying with backoff", error)
                            delay(5000)
                            Result.failure(error)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyRateLimitBackoff() {
        consecutiveRateLimitErrors++
        val currentTime = System.currentTimeMillis()
        lastRateLimitTime = currentTime

        // Calculate exponential backoff with jitter
        val baseDelay = 60_000L // 1 minute base
        val backoffDelay = (baseDelay * (2.0.pow(consecutiveRateLimitErrors - 1))).toLong()
        val maxDelay = 600_000L // 10 minutes max
        val actualDelay = min(backoffDelay, maxDelay)

        // Add some jitter (±10%)
        val jitter = (actualDelay * 0.1 * (Math.random() - 0.5)).toLong()
        val finalDelay = actualDelay + jitter

        logger.warn("🚦 Rate limit exceeded (attempt #$consecutiveRateLimitErrors). Backing off for ${finalDelay / 1000} seconds...")

        // Apply the backoff delay
        delay(finalDelay)

        logger.info("🟢 Rate limit backoff complete. Resuming...")
    }

    override fun hasMoreProjects(): Boolean {
        return stateManager.hasMorePages()
    }

    override suspend fun resetPagination() {
        // Also reset rate limiting state when pagination is reset
        consecutiveRateLimitErrors = 0
        lastRateLimitTime = 0L
        stateManager.resetCursor()
    }
}
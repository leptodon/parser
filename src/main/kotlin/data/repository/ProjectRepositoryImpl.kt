package data.repository

import data.api.AuthException
import data.api.KickstarterApi
import data.repository.mapping.ProjectDetailsMapper
import data.repository.mapping.ProjectMapper
import data.storage.StateManager
import domain.model.Project
import domain.model.ProjectDetails
import domain.repository.ProjectRepository
import kotlinx.coroutines.delay

class ProjectRepositoryImpl(
    private val api: KickstarterApi,
    private val stateManager: StateManager,
    private val projectMapper: ProjectMapper,
    private val projectDetailsMapper: ProjectDetailsMapper
) : ProjectRepository {

    override suspend fun getProjects(limit: Int): Result<List<Project>> {
        val cursor = stateManager.getLastCursor()
        return try {
            val response = api.getProjects(cursor, limit)
            response.fold(
                onSuccess = { projectsResponse ->
                    val projects = projectMapper.mapFromResponse(projectsResponse)
                    val lastCursor = projectsResponse.data.projects.pageInfo.endCursor
                    stateManager.saveLastCursor(lastCursor)

                    Result.success(projects)
                },
                onFailure = { error ->
                    if (error is AuthException) {
                        Result.failure(error)
                    } else {
                        // Retry with exponential backoff
                        delay(5000)
                        Result.failure(error)
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
                    val projectDetails = projectDetailsMapper.mapFromResponse(detailsResponse)
                    Result.success(projectDetails)
                },
                onFailure = { error ->
                    if (error is AuthException) {
                        Result.failure(error)
                    } else {
                        // Retry with exponential backoff
                        delay(5000)
                        Result.failure(error)
                    }
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun hasMoreProjects(): Boolean {
        return stateManager.hasMorePages()
    }

    override suspend fun resetPagination() {
        stateManager.resetCursor()
    }
}
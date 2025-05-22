package domain.repository

import domain.model.Project
import domain.model.ProjectDetails

interface ProjectRepository {
    suspend fun getProjects(limit: Int): Result<List<Project>>
    suspend fun getProjectDetails(slug: String): Result<ProjectDetails>
    fun hasMoreProjects(): Boolean
    suspend fun resetPagination()
}

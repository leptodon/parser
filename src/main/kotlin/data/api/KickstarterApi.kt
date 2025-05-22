package data.api

import data.api.model.ProjectDetailsResponse
import data.api.model.ProjectsResponse

interface KickstarterApi {
    suspend fun getProjects(cursor: String?, limit: Int): Result<ProjectsResponse>
    suspend fun getProjectDetails(slug: String): Result<ProjectDetailsResponse>
    suspend fun refreshToken(token: String): Result<String>
}
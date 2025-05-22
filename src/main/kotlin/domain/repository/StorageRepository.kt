package domain.repository

import domain.model.ProjectDetails

interface StorageRepository {
    suspend fun saveProjectData(projectDetails: ProjectDetails): Result<Unit>
}

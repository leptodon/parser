package domain.usecase

import domain.model.Project
import domain.repository.ProjectRepository

class GetProjectsUseCase(private val projectRepository: ProjectRepository) {
    suspend operator fun invoke(limit: Int): Result<List<Project>> {
        return projectRepository.getProjects(limit)
    }

    fun hasMoreProjects(): Boolean {
        return projectRepository.hasMoreProjects()
    }

    suspend fun resetPagination() {
        projectRepository.resetPagination()
    }
}
package domain.usecase

import domain.model.ProjectDetails
import domain.repository.ProjectRepository


class GetProjectDetailsUseCase(private val projectRepository: ProjectRepository) {
    suspend operator fun invoke(slug: String): Result<ProjectDetails> {
        return projectRepository.getProjectDetails(slug)
    }
}
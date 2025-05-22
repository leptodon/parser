package domain.usecase

import domain.model.ProjectDetails
import domain.repository.StorageRepository

class SaveProjectsDataUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(projectDetails: ProjectDetails): Result<Unit> {
        return storageRepository.saveProjectData(projectDetails)
    }
}
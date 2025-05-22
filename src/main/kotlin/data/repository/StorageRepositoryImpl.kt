package data.repository

import data.storage.CsvWriter
import domain.model.ProjectDetails
import domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepositoryImpl(
    private val csvWriter: CsvWriter
) : StorageRepository {

    override suspend fun saveProjectData(projectDetails: ProjectDetails): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            csvWriter.writeProjectDetails(projectDetails)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

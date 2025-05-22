package data.repository

import data.storage.CsvWriter
import data.storage.SimplifiedDataExporter
import domain.model.ProjectDetails
import domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepositoryImpl(
    private val csvWriter: CsvWriter,
    private val simplifiedDataExporter: SimplifiedDataExporter
) : StorageRepository {

    override suspend fun saveProjectData(projectDetails: ProjectDetails): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Сохраняем в старом формате для совместимости
            csvWriter.writeProjectDetails(projectDetails)

            // Сохраняем в новом формате для ML
            simplifiedDataExporter.addProject(projectDetails)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOutputDirectory(): String {
        return simplifiedDataExporter.getOutputDirectory()
    }
}

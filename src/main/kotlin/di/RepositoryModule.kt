package di

import data.repository.AuthRepositoryImpl
import data.repository.ProjectRepositoryImpl
import data.repository.StorageRepositoryImpl
import data.repository.mapping.ProjectDetailsMapper
import data.repository.mapping.ProjectMapper
import data.storage.CsvWriter
import data.storage.PreferencesManager
import data.storage.StateManager
import domain.repository.AuthRepository
import domain.repository.ProjectRepository
import domain.repository.StorageRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object RepositoryModule {
    val module = module {
        // Storage
        singleOf(::PreferencesManager)
        singleOf(::StateManager)
        singleOf(::CsvWriter)

        // Mappers
        singleOf(::ProjectMapper)
        singleOf(::ProjectDetailsMapper)

        // Repositories
        single<ProjectRepository> { ProjectRepositoryImpl(get(), get(), get(), get()) }
        single<StorageRepository> { StorageRepositoryImpl(get()) }

        // AuthRepository создается последним, так как он зависит от KickstarterApi
        single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    }
}
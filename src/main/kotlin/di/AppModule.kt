package di

import domain.usecase.GetProjectDetailsUseCase
import domain.usecase.GetProjectsUseCase
import domain.usecase.SaveProjectsDataUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import presentation.ParserController
import presentation.cli.CommandLineInterface

object AppModule {
    val module = module {
        // Use cases
        single { GetProjectsUseCase(get()) }
        single { GetProjectDetailsUseCase(get()) }
        single { SaveProjectsDataUseCase(get()) }

        // Controllers
        singleOf(::ParserController)
        singleOf(::CommandLineInterface)
    }

    fun provideCommandLineInterface(): CommandLineInterface {
        return org.koin.java.KoinJavaComponent.get(CommandLineInterface::class.java)
    }
}
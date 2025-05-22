import di.AppModule
import di.NetworkModule
import di.RepositoryModule
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting Kickstarter Parser")

    // Initialize dependency injection с правильным порядком модулей
    startKoin {
        modules(
            AppModule.module,
            // Сначала NetworkModule, так как RepositoryModule зависит от него
            NetworkModule.module,
            RepositoryModule.module
        )
    }

    // Start CLI
    val cli = AppModule.provideCommandLineInterface()
    cli.start()
}
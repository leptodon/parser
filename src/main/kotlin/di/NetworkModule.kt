package di

import data.api.AuthInterceptor
import data.api.KickstarterApi
import data.api.KickstarterApiImpl
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

object NetworkModule {
    val module = module {
        single { AuthInterceptor(get()) }

        single {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        json = Json {
                            ignoreUnknownKeys = true
                            coerceInputValues = true
                            isLenient = true
                        }
                    )
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.HEADERS
                }

                // Базовые заголовки, которые не требуют асинхронных операций
                install(DefaultRequest) {
                    header("User-Agent", "Kickstarter Android Mobile Variant/externalRelease Code/2014150939 Version/3.31.1")
                    header("X-KICKSTARTER-CLIENT", "6B5W0CGU6NQPQ67588QEU1DOQL19BPF521VGPNY3XQXXUEGTND")
                    header("Kickstarter-Android-App-UUID", "eiYleJuAR7a-eWh2YQ3xFR")
                    header("Kickstarter-Android-App", "2014150939")
                    header("Kickstarter-App-Id", "com.kickstarter.kickstarter")
                    header("Accept-Language", "en")
                }
            }
        }

        // И наконец KickstarterApi, который зависит от HttpClient и AuthInterceptor
        single<KickstarterApi> { KickstarterApiImpl(get(), get()) }
    }
}
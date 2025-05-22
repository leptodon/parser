plugins {
    // Для Kotlin
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

group = "com.cactus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Логирование
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.13")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Koin DI
    implementation("io.insert-koin:koin-core:4.0.4")

    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-logging:3.1.3")
    implementation("io.ktor:ktor-client-serialization:3.1.3")
    implementation("io.ktor:ktor-client-okhttp-jvm:3.1.3")

    // Для работы с файлами CSV (если нужен внешний CSV writer, например OpenCSV - но для простого вывода можно обойтись стандартным API)
    // implementation("com.opencsv:opencsv:5.7.1")

}
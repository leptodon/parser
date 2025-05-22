plugins {
    // Для Kotlin
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20" apply false
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
    // Ktor HTTP-клиент
    implementation("io.ktor:ktor-client-core:2.3.2")
    implementation("io.ktor:ktor-client-cio:2.3.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
    implementation("io.ktor:ktor-client-logging:2.3.2")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Коррутины
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Логирование
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.13")

    // Для работы с файлами CSV (если нужен внешний CSV writer, например OpenCSV - но для простого вывода можно обойтись стандартным API)
    // implementation("com.opencsv:opencsv:5.7.1")

}

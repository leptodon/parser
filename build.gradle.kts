plugins {
    // Для Kotlin
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"

    // Для создания исполняемого JAR
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

// Настройка main класса для application plugin
application {
    mainClass.set("ApplicationKt")
}

// Настройка Shadow JAR (fat JAR со всеми зависимостями)
tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("kickstarter-parser")

    // Настройка манифеста
    manifest {
        attributes(
            "Main-Class" to "ApplicationKt",
            "Implementation-Title" to "Kickstarter Parser",
            "Implementation-Version" to project.version
        )
    }

    // Исключаем подписи из JAR файлов (может вызывать проблемы)
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

// Настройка обычного JAR
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ApplicationKt",
            "Implementation-Title" to "Kickstarter Parser",
            "Implementation-Version" to project.version
        )
    }

    // Делаем JAR исполняемым (хотя без зависимостей он не запустится)
    archiveBaseName.set("kickstarter-parser-thin")
}

// Создаем задачу для создания полного исполняемого JAR
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Creates a fat JAR with all dependencies"

    archiveClassifier.set("all")
    archiveBaseName.set("kickstarter-parser")

    manifest {
        attributes(
            "Main-Class" to "ApplicationKt",
            "Implementation-Title" to "Kickstarter Parser",
            "Implementation-Version" to project.version
        )
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Настройка задач сборки
tasks.build {
    dependsOn(tasks.shadowJar)
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

    // Ktor Client
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
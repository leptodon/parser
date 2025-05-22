package utils

import domain.model.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object JsonHelper {
    private val json = Json { prettyPrint = true }
    fun saveProjects(projects: List<Project>, dir: String) {
        val file = File("$dir/kickstarter_projects.json")
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(projects))
    }
}

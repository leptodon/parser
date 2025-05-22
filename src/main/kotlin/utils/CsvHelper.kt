package utils

import domain.model.Project
import java.io.File

object CsvHelper {
    fun saveProjects(projects: List<Project>, dir: String) {
        val file = File("$dir/kickstarter_projects.csv")
        file.parentFile.mkdirs()
        file.bufferedWriter().use { out ->
            out.write("id;name;category;goal;pledged;state;backers;creator;country;location\n")
            projects.forEach { p ->
                out.write(listOf(
                    p.id, p.name, p.category.name, p.goal.amount, p.pledged.amount,
                    p.state, p.backersCount, p.creator.name, p.country, p.location?.displayableName
                ).joinToString(";") + "\n")
            }
        }
    }
}

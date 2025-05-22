package app

import data.api.KickstarterApiClient
import data.mapping.mapApiToProject
import domain.model.Project
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import utils.CsvHelper
import utils.JsonHelper
import utils.Logger

class ConsoleApp(private val api: KickstarterApiClient) {
    fun run() = runBlocking {
        println("Kickstarter Project Parser (Kotlin)")
        print("Введите количество проектов для парсинга (Enter — 15): ")
        val limit = readlnOrNull()?.toIntOrNull() ?: 15
        print("Введите путь для сохранения (по умолчанию: ./kickstarter_data): ")
        val path = readlnOrNull().takeIf { !it.isNullOrBlank() } ?: "./kickstarter_data"

        val projects = mutableListOf<Project>()
        var cursor: String? = null

        while (projects.size < limit) {
            val page = api.fetchProjects(cursor)
            println("DEBUG: page.keys = ${page.keys}")
            val edges = page["data"]!!
                .jsonObject["projects"]!!.jsonObject["edges"]!!.jsonArray
            val pageInfo = page["data"]!!
                .jsonObject["projects"]!!.jsonObject["pageInfo"]!!.jsonObject
            cursor = pageInfo["endCursor"]?.jsonPrimitive?.content

            for (edge in edges) {
                if (projects.size >= limit) break
                val node = edge.jsonObject["node"]!!.jsonObject
                val slug = node["slug"]!!.jsonPrimitive.content
                Logger.info("Парсим проект $slug ...")
                try {
                    val projectJson = api.fetchProjectDetails(slug)
                    val creatorJson = api.fetchCreatorDetails(slug)
                    val rewardsJson = api.fetchProjectRewards(slug)
                    val project = mapApiToProject(projectJson, creatorJson, rewardsJson)
                    projects.add(project)
                    Logger.info("Проект ${project.name} обработан.")
                } catch (e: Exception) {
                    Logger.error("Ошибка парсинга $slug: ${e.message}")
                }
            }
            if (pageInfo["hasNextPage"]?.jsonPrimitive?.boolean != true) break
        }
        println("Собрано проектов: ${projects.size}")
        CsvHelper.saveProjects(projects, path)
        JsonHelper.saveProjects(projects, path)
        println("Данные сохранены в $path")
    }
}

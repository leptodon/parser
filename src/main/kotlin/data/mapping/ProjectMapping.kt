package data.mapping

import domain.model.*
import kotlinx.serialization.json.*

fun mapApiToProject(
    projectJson: JsonObject,
    creatorJson: JsonObject,
    rewardsJson: JsonObject
): Project {
    val p = projectJson["data"]!!.jsonObject["project"]!!.jsonObject
    val c = creatorJson["data"]!!.jsonObject["project"]!!.jsonObject["creator"]!!.jsonObject
    val rewardsArr = rewardsJson["data"]!!
        .jsonObject["project"]!!.jsonObject["rewards"]!!.jsonObject["nodes"]!!.jsonArray

    val rewards = rewardsArr.map { r ->
        Reward(
            id = r.jsonObject["id"]!!.jsonPrimitive.content,
            name = r.jsonObject["name"]!!.jsonPrimitive.content,
            description = r.jsonObject["description"]!!.jsonPrimitive.content,
            amount = r.jsonObject["amount"]!!.jsonObject["amount"]!!.jsonPrimitive.double,
            currency = r.jsonObject["amount"]!!.jsonObject["currency"]!!.jsonPrimitive.content,
            backersCount = r.jsonObject["backersCount"]!!.jsonPrimitive.int,
            available = r.jsonObject["available"]!!.jsonPrimitive.boolean,
            estimatedDeliveryOn = r.jsonObject["estimatedDeliveryOn"]?.jsonPrimitive?.content
        )
    }

    val goalObj = p["goal"]!!.jsonObject
    val pledgedObj = p["pledged"]!!.jsonObject

    return Project(
        id = p["id"]!!.jsonPrimitive.content,
        name = p["name"]!!.jsonPrimitive.content,
        slug = p["slug"]!!.jsonPrimitive.content,
        url = p["url"]!!.jsonPrimitive.content,
        description = p["description"]!!.jsonPrimitive.content,
        story = p["story"]?.jsonPrimitive?.content ?: "",
        risks = p["risks"]?.jsonPrimitive?.content,
        category = Category(
            name = p["category"]!!.jsonObject["name"]!!.jsonPrimitive.content,
            slug = p["category"]!!.jsonObject["slug"]!!.jsonPrimitive.content,
            parentCategory = p["category"]!!.jsonObject["parentCategory"]?.jsonObject?.let { par ->
                ParentCategory(
                    name = par["name"]!!.jsonPrimitive.content,
                    slug = par["slug"]!!.jsonPrimitive.content
                )
            }
        ),
        country = p["country"]!!.jsonObject["code"]!!.jsonPrimitive.content,
        location = p["location"]?.jsonObject?.let { loc ->
            Location(
                displayableName = loc["displayableName"]?.jsonPrimitive?.content,
                country = loc["country"]?.jsonPrimitive?.content,
                name = loc["name"]?.jsonPrimitive?.content
            )
        },
        createdAt = p["createdAt"]!!.jsonPrimitive.long,
        launchedAt = p["launchedAt"]!!.jsonPrimitive.long,
        deadlineAt = p["deadlineAt"]!!.jsonPrimitive.long,
        goal = Money(
            amount = goalObj["amount"]!!.jsonPrimitive.double,
            currency = goalObj["currency"]!!.jsonPrimitive.content,
            symbol = goalObj["symbol"]!!.jsonPrimitive.content
        ),
        pledged = Money(
            amount = pledgedObj["amount"]!!.jsonPrimitive.double,
            currency = pledgedObj["currency"]!!.jsonPrimitive.content,
            symbol = pledgedObj["symbol"]!!.jsonPrimitive.content
        ),
        usdExchangeRate = p["usdExchangeRate"]?.jsonPrimitive?.double ?: 1.0,
        state = p["state"]!!.jsonPrimitive.content,
        backersCount = p["backersCount"]!!.jsonPrimitive.int,
        commentsCount = p["commentsCount"]!!.jsonPrimitive.int,
        updatesCount = p["posts"]!!.jsonObject["totalCount"]!!.jsonPrimitive.int,
        rewards = rewards,
        minPledge = p["minPledge"]?.jsonPrimitive?.double,
        prelaunchActivated = p["prelaunchActivated"]!!.jsonPrimitive.boolean,
        isProjectWeLove = p["isProjectWeLove"]!!.jsonPrimitive.boolean,
        hasVideo = p["video"] != null,
        creator = Creator(
            name = p["creator"]!!.jsonObject["name"]!!.jsonPrimitive.content,
            backingsCount = c["backingsCount"]!!.jsonPrimitive.int,
            launchedProjects = c["launchedProjects"]!!.jsonObject["totalCount"]!!.jsonPrimitive.int
        ),
        percentFunded = pledgedObj["amount"]!!.jsonPrimitive.double /
                (goalObj["amount"]!!.jsonPrimitive.double.takeIf { it != 0.0 } ?: 1.0) * 100
    )
}

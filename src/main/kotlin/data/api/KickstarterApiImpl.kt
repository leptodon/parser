package data.api

import data.api.model.GraphQLRequest
import data.api.model.ProjectDetailsResponse
import data.api.model.ProjectsResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

class KickstarterApiImpl(
    private val httpClient: HttpClient,
    private val authInterceptor: AuthInterceptor
) : KickstarterApi {

    private val logger = LoggerFactory.getLogger(KickstarterApiImpl::class.java)

    companion object {
        private const val BASE_URL = "https://www.kickstarter.com/graph"
        private const val PROJECTS_QUERY = """
            query FetchProjects(${'$'}first: Int = 15, ${'$'}cursor: String, ${'$'}sort: ProjectSort) { 
              projects(first: ${'$'}first, after: ${'$'}cursor, sort: ${'$'}sort) { 
                edges { 
                  cursor 
                  node { 
                    __typename 
                    backersCount 
                    description
                    id
                    pid
                    name
                    slug
                    isLaunched 
                    isPledgeOverTimeAllowed 
                    category {
                      name
                      parentCategory {
                        name
                      }
                    }
                    country {
                      code
                      name
                    }
                    createdAt
                    creator {
                      name
                      id
                    }
                    deadlineAt
                    goal {
                      amount
                      currency
                      symbol
                    }
                    pledged {
                      amount
                      currency
                      symbol
                    }
                    percentFunded
                    launchedAt
                    location {
                      displayableName
                    }
                    isProjectWeLove
                    state
                  } 
                } 
                pageInfo { 
                  hasPreviousPage 
                  hasNextPage 
                  startCursor 
                  endCursor 
                }
                totalCount
              } 
            }
        """

        private const val PROJECT_DETAILS_QUERY = """
            query FetchProject(${'$'}slug: String!) { 
              project(slug: ${'$'}slug) { 
                __typename 
                backersCount 
                description 
                minPledge 
                isLaunched 
                category {
                  name
                  parentCategory {
                    name
                  }
                }
                commentsCount
                country {
                  code
                  name
                }
                createdAt
                creator {
                  name
                  backingsCount
                  launchedProjects {
                    totalCount
                  }
                }
                currency
                deadlineAt
                goal {
                  amount
                  currency
                  symbol
                }
                id
                launchedAt
                location {
                  displayableName
                }
                name
                pledged {
                  amount
                  currency
                  symbol
                }
                rewards {
                  nodes {
                    id
                    name
                    backersCount
                    description
                    estimatedDeliveryOn
                    available
                    amount {
                      amount
                      currency
                      symbol
                    }
                    shippingPreference
                    remainingQuantity
                    limit
                    limitPerBacker
                    startsAt
                    endsAt
                    simpleShippingRulesExpanded {
                      locationName
                    }
                  }
                }
                risks
                story
                slug
                isProjectWeLove
                state
                stateChangedAt
                posts {
                  totalCount
                }
                video {
                  videoSources {
                    high {
                      src
                    }
                  }
                }
                faqs {
                  nodes {
                    id
                  }
                }
                environmentalCommitments {
                  commitmentCategory
                  description
                }
              } 
            }
        """
    }

    override suspend fun getProjects(cursor: String?, limit: Int): Result<ProjectsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = GraphQLRequest(
                operationName = "FetchProjects",
                variables = buildJsonObject {
                    put("sort", "MAGIC")
                    put("first", limit)
                    cursor?.let { put("cursor", it) }
                },
                query = PROJECTS_QUERY
            )

            val response = httpClient.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                setBody(request)

                // Добавляем заголовок авторизации, если он доступен
                authInterceptor.getAuthorizationHeader()?.let {
                    header("Authorization", it)
                }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch projects: ${response.status}"))
            }
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.Unauthorized -> {
                            // Очищаем кэшированный токен, так как он недействителен
                            authInterceptor.clearCachedToken()
                            Result.failure(AuthException("Authorization required"))
                        }
                        HttpStatusCode.TooManyRequests -> {
                            logger.warn("Rate limit exceeded for projects API")
                            Result.failure(RateLimitException("Rate limit exceeded - need to slow down requests"))
                        }
                        else -> {
                            logger.error("HTTP error ${e.response.status} when fetching projects")
                            Result.failure(e)
                        }
                    }
                }
                else -> Result.failure(e)
            }
        }
    }

    override suspend fun getProjectDetails(slug: String): Result<ProjectDetailsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = GraphQLRequest(
                operationName = "FetchProject",
                variables = buildJsonObject{put("slug", slug)},
                query = PROJECT_DETAILS_QUERY
            )

            val response = httpClient.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                setBody(request)

                // Добавляем заголовок авторизации, если он доступен
                authInterceptor.getAuthorizationHeader()?.let {
                    header("Authorization", it)
                }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch project details: ${response.status}"))
            }
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.Unauthorized -> {
                            // Очищаем кэшированный токен, так как он недействителен
                            authInterceptor.clearCachedToken()
                            Result.failure(AuthException("Authorization required"))
                        }
                        HttpStatusCode.TooManyRequests -> {
                            logger.warn("Rate limit exceeded for project details API: $slug")
                            Result.failure(RateLimitException("Rate limit exceeded - need to slow down requests"))
                        }
                        else -> {
                            logger.error("HTTP error ${e.response.status} when fetching project details for $slug")
                            Result.failure(e)
                        }
                    }
                }
                else -> Result.failure(e)
            }
        }
    }

    override suspend fun refreshToken(token: String): Result<String> {
        // Implement token refresh logic
        return Result.failure(NotImplementedError("Token refresh not implemented"))
    }
}

class AuthException(message: String) : Exception(message)
class RateLimitException(message: String) : Exception(message)
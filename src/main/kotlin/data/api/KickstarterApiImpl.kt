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
        private const val CLIENT_ID = "6B5W0CGU6NQPQ67588QEU1DOQL19BPF521VGPNY3XQXXUEGTND"

        // Добавляем более консервативные задержки между запросами
        private const val MIN_REQUEST_DELAY = 2000L // 2 секунды между запросами
        private const val RATE_LIMIT_DELAY = 120_000L // 30 секунд при получении 429

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

    // Добавляем отслеживание времени последнего запроса для rate limiting
    private var lastRequestTime = 0L

    private suspend fun applyRateLimit() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime

        if (timeSinceLastRequest < MIN_REQUEST_DELAY) {
            val delayNeeded = MIN_REQUEST_DELAY - timeSinceLastRequest
            logger.debug("Applying rate limit delay: ${delayNeeded}ms")
            kotlinx.coroutines.delay(delayNeeded)
        }

        lastRequestTime = System.currentTimeMillis()
    }

    private fun HttpRequestBuilder.setKickstarterHeaders() {
        // Основные заголовки из анализа REST API запросов
        header("Accept", "application/json")
        header("User-Agent", "Kickstarter Android Mobile Variant/externalRelease Code/2014150939 Version/3.31.1")
        header("X-KICKSTARTER-CLIENT", CLIENT_ID)
        header("Kickstarter-Android-App-UUID", "eiYleJuAR7a-eWh2YQ3xFR")
        header("Kickstarter-Android-App", "2014150939")
        header("Kickstarter-App-Id", "com.kickstarter.kickstarter")
        header("Accept-Language", "en")

        // Добавляем токен авторизации, если доступен
        authInterceptor.getAuthorizationHeader()?.let { token ->
            // Используем формат X-Auth: token <token> как в REST API
            header("X-Auth", "token $token")
        }

        // Добавляем Content-Type для GraphQL
        contentType(ContentType.Application.Json)
    }

    override suspend fun getProjects(cursor: String?, limit: Int): Result<ProjectsResponse> = withContext(Dispatchers.IO) {
        try {
            // Применяем rate limiting
            applyRateLimit()

            val request = GraphQLRequest(
                operationName = "FetchProjects",
                variables = buildJsonObject {
                    put("sort", "MAGIC")
                    put("first", limit)
                    cursor?.let { put("cursor", it) }
                },
                query = PROJECTS_QUERY
            )

            logger.debug("Making projects request with cursor: $cursor, limit: $limit")

            val response = httpClient.post(BASE_URL) {
                setKickstarterHeaders()
                setBody(request)
            }

            if (response.status.isSuccess()) {
                logger.debug("Projects request successful: ${response.status}")
                Result.success(response.body())
            } else {
                logger.warn("Projects request failed with status: ${response.status}")
                Result.failure(Exception("Failed to fetch projects: ${response.status}"))
            }
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.Unauthorized -> {
                            logger.error("Unauthorized request - token may be invalid")
                            authInterceptor.clearCachedToken()
                            Result.failure(AuthException("Authorization required - token invalid"))
                        }
                        HttpStatusCode.TooManyRequests -> {
                            logger.warn("Rate limit exceeded for projects API - need longer delays")
                            // Применяем более длительную задержку при rate limiting
                            kotlinx.coroutines.delay(RATE_LIMIT_DELAY)
                            Result.failure(RateLimitException("Rate limit exceeded - applied ${RATE_LIMIT_DELAY}ms delay"))
                        }
                        HttpStatusCode.Forbidden -> {
                            logger.error("Forbidden request - may need different authentication")
                            Result.failure(AuthException("Access forbidden - check authentication"))
                        }
                        else -> {
                            logger.error("HTTP error ${e.response.status} when fetching projects", e)
                            Result.failure(e)
                        }
                    }
                }
                else -> {
                    logger.error("Network error when fetching projects", e)
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getProjectDetails(slug: String): Result<ProjectDetailsResponse> = withContext(Dispatchers.IO) {
        try {
            // Применяем rate limiting
            applyRateLimit()

            val request = GraphQLRequest(
                operationName = "FetchProject",
                variables = buildJsonObject { put("slug", slug) },
                query = PROJECT_DETAILS_QUERY
            )

            logger.debug("Making project details request for slug: $slug")

            val response = httpClient.post(BASE_URL) {
                setKickstarterHeaders()
                setBody(request)
            }

            if (response.status.isSuccess()) {
                logger.debug("Project details request successful for $slug: ${response.status}")
                Result.success(response.body())
            } else {
                logger.warn("Project details request failed for $slug with status: ${response.status}")
                Result.failure(Exception("Failed to fetch project details: ${response.status}"))
            }
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.Unauthorized -> {
                            logger.error("Unauthorized request for project $slug - token may be invalid")
                            authInterceptor.clearCachedToken()
                            Result.failure(AuthException("Authorization required - token invalid"))
                        }
                        HttpStatusCode.TooManyRequests -> {
                            logger.warn("Rate limit exceeded for project details API: $slug")
                            kotlinx.coroutines.delay(RATE_LIMIT_DELAY)
                            Result.failure(RateLimitException("Rate limit exceeded for $slug - applied ${RATE_LIMIT_DELAY}ms delay"))
                        }
                        HttpStatusCode.Forbidden -> {
                            logger.error("Forbidden request for project $slug - may need different authentication")
                            Result.failure(AuthException("Access forbidden for $slug - check authentication"))
                        }
                        HttpStatusCode.NotFound -> {
                            logger.warn("Project not found: $slug")
                            Result.failure(Exception("Project not found: $slug"))
                        }
                        else -> {
                            logger.error("HTTP error ${e.response.status} when fetching project details for $slug", e)
                            Result.failure(e)
                        }
                    }
                }
                else -> {
                    logger.error("Network error when fetching project details for $slug", e)
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun refreshToken(token: String): Result<String> {
        // Для GraphQL API обновление токенов может потребовать отдельного REST endpoint
        logger.warn("Token refresh not implemented for GraphQL API")
        return Result.failure(NotImplementedError("Token refresh not implemented for GraphQL API"))
    }
}

class AuthException(message: String) : Exception(message)
class RateLimitException(message: String) : Exception(message)
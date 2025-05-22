package data.repository

import data.api.AuthInterceptor
import data.api.KickstarterApi
import domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val api: KickstarterApi,
    private val authInterceptor: AuthInterceptor
) : AuthRepository {

    override suspend fun setAuthToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            authInterceptor.setAuthToken(token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuthToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = authInterceptor.getAuthorizationHeader()
            if (token.isNullOrEmpty()) {
                Result.failure(Exception("No auth token available"))
            } else {
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentToken = authInterceptor.getAuthorizationHeader()
            if (currentToken.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No auth token available to refresh"))
            }

            val result = api.refreshToken(currentToken)
            result.fold(
                onSuccess = { newToken ->
                    authInterceptor.setAuthToken(newToken)
                    Result.success(newToken)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isTokenValid(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = authInterceptor.getAuthorizationHeader()
            if (token.isNullOrEmpty()) {
                return@withContext Result.success(false)
            }

            // Упрощенная проверка валидности токена
            val isValid = token.length > 10
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
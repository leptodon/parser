package domain.repository

interface AuthRepository {
    suspend fun setAuthToken(token: String): Result<Unit>
    suspend fun getAuthToken(): Result<String>
    suspend fun refreshToken(): Result<String>
    suspend fun isTokenValid(): Result<Boolean>
}

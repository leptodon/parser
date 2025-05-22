package data.api

import data.storage.PreferencesManager
import org.slf4j.LoggerFactory

class AuthInterceptor(private val preferencesManager: PreferencesManager) {
    private val logger = LoggerFactory.getLogger(AuthInterceptor::class.java)

    companion object {
        private const val AUTH_TOKEN_KEY = "auth_token"
    }

    // Кэшированный токен для минимизации обращений к хранилищу
    @Volatile
    private var cachedToken: String? = null

    fun getAuthorizationHeader(): String? {
        // Используем кэшированный токен, если он есть
        cachedToken?.let { return it }

        return try {
            val token = preferencesManager.getString(AUTH_TOKEN_KEY, "")
            if (token.isNotEmpty()) {
                cachedToken = token
                token
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting auth token", e)
            null
        }
    }

    fun setAuthToken(token: String) {
        preferencesManager.saveString(AUTH_TOKEN_KEY, token)
        cachedToken = token
    }

    fun clearCachedToken() {
        cachedToken = null
    }
}
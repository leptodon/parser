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
        cachedToken?.let {
            logger.debug("Using cached auth token")
            return it
        }

        return try {
            val token = preferencesManager.getString(AUTH_TOKEN_KEY, "")
            if (token.isNotEmpty()) {
                cachedToken = token
                logger.debug("Loaded auth token from preferences")
                token
            } else {
                logger.warn("No auth token found in preferences")
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting auth token", e)
            null
        }
    }

    fun setAuthToken(token: String) {
        try {
            // Очищаем префикс "token " если он уже есть
            val cleanToken = token.removePrefix("token ").trim()

            preferencesManager.saveString(AUTH_TOKEN_KEY, cleanToken)
            cachedToken = cleanToken
            logger.info("Auth token updated successfully")
        } catch (e: Exception) {
            logger.error("Error saving auth token", e)
        }
    }

    fun clearCachedToken() {
        cachedToken = null
        logger.info("Cached auth token cleared")
    }

    // Добавляем метод для полной очистки токена
    fun clearAuthToken() {
        try {
            preferencesManager.saveString(AUTH_TOKEN_KEY, "")
            cachedToken = null
            logger.info("Auth token completely cleared")
        } catch (e: Exception) {
            logger.error("Error clearing auth token", e)
        }
    }

    // Метод для проверки валидности токена
    fun hasValidToken(): Boolean {
        val token = getAuthorizationHeader()
        return !token.isNullOrBlank() && token.length > 10
    }
}
package data.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StateManager(private val preferencesManager: PreferencesManager) {
    private val mutex = Mutex()
    private var hasMore: Boolean = true

    suspend fun saveLastCursor(cursor: String?) = mutex.withLock {
        preferencesManager.saveString("last_cursor", cursor ?: "")
        hasMore = cursor != null
    }

    suspend fun getLastCursor(): String? = mutex.withLock {
        val cursor = preferencesManager.getString("last_cursor", "")
        return if (cursor.isEmpty()) null else cursor
    }

    suspend fun resetCursor() = mutex.withLock {
        preferencesManager.saveString("last_cursor", "")
        hasMore = true
    }

    fun hasMorePages(): Boolean {
        return hasMore
    }
}
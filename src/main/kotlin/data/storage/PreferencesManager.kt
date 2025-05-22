package data.storage

import java.io.File
import java.util.Properties

class PreferencesManager {
    private val properties = Properties()
    private val prefsFile = File("parser_state.properties")

    init {
        if (prefsFile.exists()) {
            prefsFile.inputStream().use { properties.load(it) }
        }
    }

    fun saveString(key: String, value: String) {
        properties[key] = value
        prefsFile.outputStream().use { properties.store(it, null) }
    }

    fun getString(key: String, defaultValue: String): String {
        return properties.getProperty(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        properties[key] = value.toString()
        prefsFile.outputStream().use { properties.store(it, null) }
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return properties.getProperty(key)?.toIntOrNull() ?: defaultValue
    }

    fun saveBoolean(key: String, value: Boolean) {
        properties[key] = value.toString()
        prefsFile.outputStream().use { properties.store(it, null) }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue
    }
}
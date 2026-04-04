package org.bytestorm.bitesync.settings

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromString(value: String): ThemeMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

interface SettingsRepository {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getDeviceLanguageCode(): String
}

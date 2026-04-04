package org.bytestorm.bitesync.settings

import android.content.Context

class AndroidSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("bitesync_settings", Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getDeviceLanguageCode(): String =
        java.util.Locale.getDefault().language
}

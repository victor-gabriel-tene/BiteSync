package org.bytestorm.bitesync.settings

import platform.Foundation.NSUserDefaults

class IosSettingsRepository : SettingsRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, default: String): String =
        defaults.stringForKey(key) ?: default

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun getDeviceLanguageCode(): String =
        platform.Foundation.NSLocale.currentLocale.languageCode ?: "en"
}

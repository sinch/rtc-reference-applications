package com.sinch.rtc.vvc.reference.app.storage.prefs

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.sinch.rtc.vvc.reference.app.domain.AppConfig
import com.sinch.rtc.vvc.reference.app.utils.extensions.defaultConfigs

class SharedPrefsManager(private val appContext: Application) {

    companion object {
        private const val PREFS_NAME = "sinchprefs"
        private const val APP_KEY = "app_key"
        private const val APP_SECRET_KEY = "app_secret"
        private const val ENV_KEY = "environment"
        private const val IS_CUSTOM_ENV_KEY = "is_custom_key"
        private const val USED_DEF_NAME_KEY = "def_pos_key"
        private const val FCM_REG_TOKEN_KEY = "fcm_token"
    }

    private val preferences = customPrefs(appContext, PREFS_NAME)

    val defaultConfigs: List<AppConfig> get() = appContext.defaultConfigs

    var usedConfig: AppConfig
        get() {
            return if (isCustomConfigUsed) {
                AppConfig(
                    AppConfig.CUSTOM_CONFIG_NAME,
                    customAppKey,
                    customAppSecret,
                    customEnvironment,
                    "",
                    true
                )
            } else {
                defaultConfigs.firstOrNull { it.name == usedDefaultConfigName }
                    ?: defaultConfigs.first()
            }
        }
        set(value) {
            isCustomConfigUsed = value.isCustom
            if (value.isCustom) {
                usedDefaultConfigName = ""
                customAppKey = value.appKey
                customAppSecret = value.appSecret
                customEnvironment = value.environment
            } else {
                usedDefaultConfigName = value.name
            }
        }

    private var usedDefaultConfigName: String
        get() = preferences[USED_DEF_NAME_KEY, ""]
        set(value) {
            preferences[USED_DEF_NAME_KEY] = value
        }

    private var isCustomConfigUsed: Boolean
        get() = preferences[IS_CUSTOM_ENV_KEY, false]
        set(value) {
            preferences[IS_CUSTOM_ENV_KEY] = value
        }

    private var customAppKey: String
        get() = preferences[APP_KEY, ""]
        set(value) {
            preferences[APP_KEY] = value
        }

    private var customAppSecret: String
        get() = preferences[APP_SECRET_KEY, ""]
        set(value) {
            preferences[APP_SECRET_KEY] = value
        }

    private var customEnvironment: String
        get() = preferences[ENV_KEY, ""]
        set(value) {
            preferences[ENV_KEY] = value
        }

    var fcmRegistrationToken: String
        get() = preferences[FCM_REG_TOKEN_KEY, ""]
        set(value) {
            preferences[FCM_REG_TOKEN_KEY] = value
        }

    private fun customPrefs(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = this.edit()
        operation(editor)
        editor.apply()
    }

    operator fun SharedPreferences.set(key: String, value: Any?) = when (value) {
        is String? -> edit { it.putString(key, value) }
        is Int -> edit { it.putInt(key, value) }
        is Boolean -> edit { it.putBoolean(key, value) }
        is Float -> edit { it.putFloat(key, value) }
        is Long -> edit { it.putLong(key, value) }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }

    inline operator fun <reified T : Any> SharedPreferences.get(
        key: String,
        defaultValue: T? = null
    ): T = when (T::class) {
        String::class -> getString(key, defaultValue as? String ?: "") as T
        Int::class -> getInt(key, defaultValue as? Int ?: -1) as T
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T
        Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T
        Long::class -> getLong(key, defaultValue as? Long ?: -1) as T
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}
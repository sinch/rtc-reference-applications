package com.sinch.rtc.vvc.reference.app.storage.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.sinch.rtc.vvc.reference.app.application.Constants

class SharedPrefsManager(context: Context) {

    companion object {
        private const val APP_KEY = "app_key"
        private const val APP_SECRET_KEY = "app_secret"
        private const val ENV_KEY = "environment"
    }

    private val preferences = defaultPrefs(context)

    var appKey: String
        get() = preferences[APP_KEY, Constants.APP_KEY]
        set(value) {
            preferences[APP_KEY] = value
        }

    var appSecret: String
        get() = preferences[APP_SECRET_KEY, Constants.APP_SECRET]
        set(value) {
            preferences[APP_SECRET_KEY] = value
        }

    var environment: String
        get() = preferences[ENV_KEY, Constants.ENVIRONMENT]
        set(value) {
            preferences[ENV_KEY] = value
        }

    private fun defaultPrefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

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
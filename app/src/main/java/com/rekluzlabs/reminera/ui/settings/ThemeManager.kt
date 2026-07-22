package com.rekluzlabs.reminera.ui.settings

import android.content.Context
import android.content.SharedPreferences

class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.WARM_TERRACOTTA.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.WARM_TERRACOTTA.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.WARM_TERRACOTTA
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "reminera_settings"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}

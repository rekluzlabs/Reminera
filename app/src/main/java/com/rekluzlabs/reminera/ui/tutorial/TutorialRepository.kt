package com.rekluzlabs.reminera.ui.tutorial

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TutorialRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)

    fun isSeen(screenKey: String): Boolean {
        return prefs.getBoolean("tutorial_${screenKey}_seen", false)
    }

    fun markSeen(screenKey: String) {
        prefs.edit { putBoolean("tutorial_${screenKey}_seen", true) }
    }

    fun resetAll() {
        prefs.edit { clear() }
    }
}

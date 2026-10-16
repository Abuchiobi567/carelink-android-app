package com.example.carelink.data

import android.content.Context
import android.content.SharedPreferences

// keeps the logged-in user and theme choice between app launches
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(userId: Long, role: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_ROLE, role)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    // sign-out: drop session keys but keep the theme choice
    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_ROLE)
            .remove(KEY_IS_LOGGED_IN)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "") ?: ""

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    // wipes the override so the app falls back to "follow system theme"
    fun clearThemeOverride() {
        prefs.edit().remove(KEY_DARK_MODE).apply()
    }

    // null = follow system theme
    fun getDarkModeOverride(): Boolean? {
        return if (prefs.contains(KEY_DARK_MODE)) {
            prefs.getBoolean(KEY_DARK_MODE, false)
        } else null
    }

    companion object {
        const val PREFS_NAME = "carelink_prefs"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
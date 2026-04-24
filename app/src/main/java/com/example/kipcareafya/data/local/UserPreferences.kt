package com.example.kipcareafya.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_ID       = intPreferencesKey("user_id")
        val USERNAME      = stringPreferencesKey("username")
        val USER_EMAIL    = stringPreferencesKey("user_email")
        val DARK_MODE     = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    val userId: Flow<Int> =
        context.dataStore.data.map { it[USER_ID] ?: -1 }

    val username: Flow<String> =
        context.dataStore.data.map { it[USERNAME] ?: "" }

    val userEmail: Flow<String> =
        context.dataStore.data.map { it[USER_EMAIL] ?: "" }

    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { (it[USER_ID] ?: -1) > 0 }

    val isDarkMode: Flow<Boolean> =
        context.dataStore.data.map { it[DARK_MODE] ?: false }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATIONS] ?: true }

    suspend fun saveUser(id: Int, name: String, email: String = "") {
        context.dataStore.edit { prefs ->
            prefs[USER_ID]    = id
            prefs[USERNAME]   = name
            prefs[USER_EMAIL] = email
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID)
            prefs.remove(USERNAME)
            prefs.remove(USER_EMAIL)
        }
    }
}
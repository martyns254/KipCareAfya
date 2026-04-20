package com.example.kipcareafya.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
    }

    val userId: Flow<Int> = context.dataStore.data.map { it[USER_ID] ?: -1 }
    val username: Flow<String> = context.dataStore.data.map { it[USERNAME] ?: "" }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { (it[USER_ID] ?: -1) > 0 }

    suspend fun saveUser(id: Int, name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[USERNAME] = name
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}
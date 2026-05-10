package com.app.nepallivetv.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.appDataStore by preferencesDataStore(name = "app_preferences")

class DatastorePreferences(private val context: Context) {
    private val THEME_KEY = booleanPreferencesKey("is_dark_mode")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_channels")
    private val CAST_ENABLED_KEY = booleanPreferencesKey("is_cast_enabled")
    
    // Auth Keys
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val USER_NAME_KEY = stringPreferencesKey("auth_user_name")
    private val USER_EMAIL_KEY = stringPreferencesKey("auth_user_email")
    private val USER_PHONE_KEY = stringPreferencesKey("auth_user_phone")

    val isDarkModeFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: true
    }

    val favoriteUrlsFlow: Flow<Set<String>> = context.appDataStore.data.map { prefs ->
        prefs[FAVORITES_KEY] ?: emptySet()
    }
    
    val isCastEnabledFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[CAST_ENABLED_KEY] ?: true
    }
    
    val authTokenFlow: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }
    
    val userNameFlow: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }
    
    val userEmailFlow: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }
    
    val userPhoneFlow: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[USER_PHONE_KEY]
    }

    suspend fun getToken(): String? {
        val prefs = context.appDataStore.data.first()
        return prefs[TOKEN_KEY]
    }

    suspend fun saveAuthData(token: String, name: String, email: String, phone: String) {
        context.appDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_NAME_KEY] = name
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_PHONE_KEY] = phone
        }
    }
    
    suspend fun clearAuthData() {
        context.appDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_PHONE_KEY)
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.appDataStore.edit { prefs -> prefs[THEME_KEY] = isDark }
    }
    
    suspend fun setCastEnabled(isEnabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[CAST_ENABLED_KEY] = isEnabled }
    }

    suspend fun toggleFavorite(encodedUrl: String) {
        context.appDataStore.edit { prefs ->
            val currentFavorites = prefs[FAVORITES_KEY] ?: emptySet()
            val newFavorites = currentFavorites.toMutableSet()
            if (newFavorites.contains(encodedUrl)) {
                newFavorites.remove(encodedUrl)
            } else {
                newFavorites.add(encodedUrl)
            }
            prefs[FAVORITES_KEY] = newFavorites
        }
    }
}

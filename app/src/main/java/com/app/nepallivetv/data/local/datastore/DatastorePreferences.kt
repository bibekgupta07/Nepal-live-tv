package com.app.nepallivetv.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_preferences")

class DatastorePreferences(private val context: Context) {
    private val THEME_KEY = booleanPreferencesKey("is_dark_mode")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_channels")
    private val CAST_ENABLED_KEY = booleanPreferencesKey("is_cast_enabled")

    val isDarkModeFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: true
    }

    val favoriteUrlsFlow: Flow<Set<String>> = context.appDataStore.data.map { prefs ->
        prefs[FAVORITES_KEY] ?: emptySet()
    }
    
    val isCastEnabledFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[CAST_ENABLED_KEY] ?: true
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

package com.app.nepallivetv.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites_prefs")

class FavoritesPreferences(private val context: Context) {
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_channels")

    val favoriteUrls: Flow<Set<String>> = context.favoritesDataStore.data.map { prefs ->
        prefs[FAVORITES_KEY] ?: emptySet()
    }

    suspend fun toggleFavorite(encodedUrl: String) {
        context.favoritesDataStore.edit { prefs ->
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

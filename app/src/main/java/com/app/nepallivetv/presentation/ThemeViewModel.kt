package com.app.nepallivetv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.local.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val themePreferences: ThemePreferences) : ViewModel() {
    
    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkModeFlow
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.Eagerly, 
            initialValue = true
        )
        
    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch { 
            themePreferences.setDarkMode(isDark) 
        }
    }
}
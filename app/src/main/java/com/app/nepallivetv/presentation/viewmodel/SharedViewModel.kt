package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.domain.model.Channel
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SharedViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    val datastorePreferences: DatastorePreferences
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    val favoriteUrls: StateFlow<Set<String>> = datastorePreferences.favoriteUrlsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val isDarkMode: StateFlow<Boolean> = datastorePreferences.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isCastEnabled: StateFlow<Boolean> = datastorePreferences.isCastEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val filteredChannels: StateFlow<List<Channel>> = combine(
        _channels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        channels.filter { channel ->
            val matchesQuery = channel.name.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || channel.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = datastorePreferences.favoriteChannelsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentStreamUrl = MutableStateFlow<String?>(null)
    val currentStreamUrl: StateFlow<String?> = _currentStreamUrl

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _playerMode = MutableStateFlow(PlayerMode.MINI)
    val playerMode: StateFlow<PlayerMode> = _playerMode

    // Backwards-compatible convenience: many call sites just want to know
    // "is the player taking the whole screen right now?"
    val isFullScreen: StateFlow<Boolean> = _playerMode
        .map { it == PlayerMode.FULL }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Fetch channels
            val fetchedChannels = getChannelsUseCase()
            _channels.value = fetchedChannels
            
            val uniqueCategories = fetchedChannels
                .map { it.category }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                
            _categories.value = listOf("All") + uniqueCategories
            
            if (fetchedChannels.isNotEmpty() && _selectedChannel.value == null) {
                selectChannel(fetchedChannels.first())
            }

            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            datastorePreferences.setDarkMode(isDark)
        }
    }

    fun setCastEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            datastorePreferences.setCastEnabled(isEnabled)
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            datastorePreferences.toggleFavorite(channel)
        }
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _currentStreamUrl.value = null
        
        viewModelScope.launch {
            val url = getStreamUrlUseCase(channel.encodedUrl)
            _currentStreamUrl.value = url
        }
    }
    
    fun setPlayerMode(mode: PlayerMode) {
        _playerMode.value = mode
    }

    /**
     * Going to fullscreen is unambiguous. Coming back, we drop to EXPANDED rather
     * than MINI — that matches the natural "I rotated back to portrait, show me
     * the half-screen view I was just in" expectation.
     */
    fun setFullScreen(fullScreen: Boolean) {
        _playerMode.value = if (fullScreen) PlayerMode.FULL else PlayerMode.EXPANDED
    }

    fun expandPlayer() { _playerMode.value = PlayerMode.EXPANDED }

    fun minimizePlayer() { _playerMode.value = PlayerMode.MINI }

    fun closePlayer() {
        _selectedChannel.value = null
        _currentStreamUrl.value = null
        _playerMode.value = PlayerMode.MINI
    }
}

package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SharedViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val datastorePreferences: DatastorePreferences
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

    val tvListChannels: StateFlow<List<Channel>> = _channels.map { channels ->
        channels.filter { channel ->
            val name = channel.name.lowercase()
            name.contains("npl live") ||
            name.contains("kantipur max hd") ||
            name.contains("kantipur hd max") ||
            channel.category.equals("Sports", ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val featuredTvListChannels: StateFlow<List<Channel>> = _channels.map { channels ->
        channels.filter { channel ->
            val name = channel.name.lowercase()
            name.contains("npl live") ||
            name.contains("star sports 1 hd") ||
            name.contains("star sports 1 sd") ||
            name.contains("star sports 1 hindi")
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = combine(
        _channels,
        favoriteUrls
    ) { channels, favs ->
        channels.filter { it.encodedUrl in favs }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectNplLive() {
        val nplLive = tvListChannels.value.find { it.name.lowercase().contains("npl live") }
        if (nplLive != null) {
            selectChannel(nplLive)
        }
    }

    private val _currentStreamUrl = MutableStateFlow<String?>(null)
    val currentStreamUrl: StateFlow<String?> = _currentStreamUrl

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
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
            datastorePreferences.toggleFavorite(channel.encodedUrl)
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
    
    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen
    }

    fun closePlayer() {
        _selectedChannel.value = null
        _currentStreamUrl.value = null
        _isFullScreen.value = false
    }
}

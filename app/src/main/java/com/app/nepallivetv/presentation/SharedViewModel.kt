package com.app.nepallivetv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * SharedViewModel manages the state for the screens like Home and TV List.
 * It follows the MVI/MVVM architectural pattern, exposing state exclusively through immutable [StateFlow]s.
 */
class SharedViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val favoritesPreferences: com.app.nepallivetv.data.local.datastore.FavoritesPreferences
) : ViewModel() {

    // --- RAW STATE ---
    // The raw, unfiltered list of all channels fetched from the repository
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    // The current search text typed by the user
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // The currently active category filter
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Dynamically generated list of categories based on available channels
    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    // Favorite channels state
    val favoriteUrls: StateFlow<Set<String>> = favoritesPreferences.favoriteUrls
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    // --- DERIVED STATE ---
    /**
     * A dynamically combined flow that recalculates the visible channels anytime the raw channels, 
     * search query, or selected category changes.
     */
    val filteredChannels: StateFlow<List<Channel>> = combine(
        _channels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        channels.filter { channel ->
            // 1. Check if the channel name matches the search query
            val matchesQuery = channel.name.contains(query, ignoreCase = true)
            
            // 2. Exact match using the channel's actual category_title from the JSON
            val matchesCategory = category == "All" || channel.category.equals(category, ignoreCase = true)
            
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tvListChannels: StateFlow<List<Channel>> = _channels.map { channels ->
        channels.filter { channel ->
            val name = channel.name.lowercase()
            name.contains("npl live") ||
            name.contains("kantipur max hd") || // Handles kantipur max hd and hd2
            name.contains("kantipur hd max") ||
            channel.category.equals("Sports", ignoreCase = true)
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

    // --- PLAYER STATE ---
    // The fully decoded, tokenized URL ready to be passed to ExoPlayer
    private val _currentStreamUrl = MutableStateFlow<String?>(null)
    val currentStreamUrl: StateFlow<String?> = _currentStreamUrl

    // The channel currently actively playing in the video player
    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel
    
    // UI Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Full screen state
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen

    init {
        loadChannels()
    }

    /**
     * Initializes the app by fetching the channels from the UseCase (Domain layer).
     */
    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetchedChannels = getChannelsUseCase()
            _channels.value = fetchedChannels
            
            // Dynamically extract all unique categories from the JSON data
            val uniqueCategories = fetchedChannels
                .map { it.category }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                
            _categories.value = listOf("All") + uniqueCategories
            
            // Automatically start playing the very first channel in the list on boot
            if (fetchedChannels.isNotEmpty() && _selectedChannel.value == null) {
                selectChannel(fetchedChannels.first())
            }

            _isLoading.value = false
        }
    }

    // --- INTENTS / USER ACTIONS ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            favoritesPreferences.toggleFavorite(channel.encodedUrl)
        }
    }

    /**
     * Selects a channel, resets the player, and decodes the secure URL.
     */
    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _currentStreamUrl.value = null // reset url briefly to trigger player recomposition
        
        viewModelScope.launch {
            // Decode the Base64 URL into a usable stream link
            val url = getStreamUrlUseCase(channel.encodedUrl)
            _currentStreamUrl.value = url
        }
    }
    
    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen
    }

    /**
     * Shuts down the player and returns to standard grid view.
     */
    fun closePlayer() {
        _selectedChannel.value = null
        _currentStreamUrl.value = null
        _isFullScreen.value = false
    }
}

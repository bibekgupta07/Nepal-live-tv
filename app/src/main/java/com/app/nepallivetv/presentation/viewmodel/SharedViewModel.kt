package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.data.model.Match
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.domain.usecase.GetCricketMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class SharedViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val getCricketMatchesUseCase: GetCricketMatchesUseCase,
    val datastorePreferences: DatastorePreferences
) : ViewModel() {

    private val _cricketMatches = MutableStateFlow<List<Match>>(emptyList())
    val cricketMatches: StateFlow<List<Match>> = _cricketMatches

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

    private var isPollingActive = false

    init {
        loadChannels()
        startDataPolling()
    }

    fun setCricketPollingActive(active: Boolean) {
        isPollingActive = active
    }

    fun refreshCricketMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val matches = getCricketMatchesUseCase()
                if (matches.isNotEmpty()) {
                    val sorted = matches.sortedWith(Comparator { m1, m2 ->
                        val p1 = getMatchPriority(m1)
                        val p2 = getMatchPriority(m2)
                        if (p1 != p2) p1.compareTo(p2) else m1.startTime.compareTo(m2.startTime)
                    })
                    _cricketMatches.value = sorted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startDataPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(15000) // Poll every 15 seconds to keep cricket data fresh
                if (!isPollingActive) continue

                try {
                    val matches = getCricketMatchesUseCase()
                    if (matches.isNotEmpty()) {
                        // Apply custom sorting (IPL / International prioritizing)
                        val sorted = matches.sortedWith(Comparator { m1, m2 ->
                            val p1 = getMatchPriority(m1)
                            val p2 = getMatchPriority(m2)
                            if (p1 != p2) p1.compareTo(p2) else m1.startTime.compareTo(m2.startTime)
                        })
                        _cricketMatches.value = sorted
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getMatchPriority(match: Match): Int {
        val title = match.title.uppercase()
        val format = match.format.uppercase()
        val subtitle = match.subtitle.uppercase()
        if (title.contains("IPL") || subtitle.contains("INDIAN PREMIER LEAGUE")) return 0
        if (format in listOf("T20I", "ODI", "TEST")) return 1
        if (title.contains("INTERNATIONAL") || subtitle.contains("INTERNATIONAL")) return 1
        if (title.contains("PSL") || title.contains("BBL") || title.contains("CPL")) return 2
        return 3 // Domestic games
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Fetch channels
            val fetchedChannels = getChannelsUseCase()
            _channels.value = fetchedChannels
            
            // Fetch cricket matches simultaneously
            try {
                val matches = getCricketMatchesUseCase()
                if (matches.isNotEmpty()) {
                    val sorted = matches.sortedWith(Comparator { m1, m2 ->
                        val p1 = getMatchPriority(m1)
                        val p2 = getMatchPriority(m2)
                        if (p1 != p2) p1.compareTo(p2) else m1.startTime.compareTo(m2.startTime)
                    })
                    _cricketMatches.value = sorted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
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

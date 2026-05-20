package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.domain.model.Channel
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.presentation.util.CategoryClassifier
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

    /** "All" plus the synthetic [CategoryClassifier.Bucket] labels. */
    val categories: StateFlow<List<String>> = MutableStateFlow(
        listOf(CATEGORY_ALL) + CategoryClassifier.Bucket.all().map { it.label }
    )

    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory

    val favoriteUrls: StateFlow<Set<String>> = datastorePreferences.favoriteUrlsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val favoriteChannels: StateFlow<List<Channel>> = datastorePreferences.favoriteChannelsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyWatched: StateFlow<List<Channel>> = datastorePreferences.recentlyWatchedFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isDarkMode: StateFlow<Boolean> = datastorePreferences.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isCastEnabled: StateFlow<Boolean> = datastorePreferences.isCastEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /**
     * Channels filtered by both the search box and the selected synthetic
     * category. The upstream `category` field is ignored — we classify by
     * name via [CategoryClassifier] because the API returns "All" for almost
     * every row.
     */
    val filteredChannels: StateFlow<List<Channel>> = combine(
        _channels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        channels.filter { channel ->
            val matchesQuery = channel.name.contains(query, ignoreCase = true)
            val matchesCategory = category == CATEGORY_ALL ||
                CategoryClassifier.classify(channel).label.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * A small rotation of channels to feature in the hero carousel. Drawn
     * from recently-watched first (personalization), then padded with
     * arbitrary live channels so a fresh install still has something to
     * showcase.
     */
    val featuredChannels: StateFlow<List<Channel>> = combine(
        _channels,
        recentlyWatched,
        favoriteChannels
    ) { all, recents, favs ->
        val seen = mutableSetOf<String>()
        val out = mutableListOf<Channel>()
        // Prefer recents (most personal), then favorites, then arbitrary HD picks.
        sequenceOf(recents, favs, all.filter { CategoryClassifier.isHd(it) }, all)
            .flatten()
            .forEach { ch ->
                if (out.size >= FEATURED_LIMIT) return@forEach
                if (seen.add(ch.encodedUrl)) out += ch
            }
        out
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentStreamUrl = MutableStateFlow<String?>(null)
    val currentStreamUrl: StateFlow<String?> = _currentStreamUrl

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _playerMode = MutableStateFlow(PlayerMode.MINI)
    val playerMode: StateFlow<PlayerMode> = _playerMode

    val isFullScreen: StateFlow<Boolean> = _playerMode
        .map { it == PlayerMode.FULL }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _channels.value = getChannelsUseCase()
            // We don't auto-select a channel on startup any more. A flat
            // auto-play surprised the user with audio on app launch and
            // forced the player into the layout regardless of intent.
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun onCategorySelected(category: String) { _selectedCategory.value = category }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch { datastorePreferences.setDarkMode(isDark) }
    }

    fun setCastEnabled(isEnabled: Boolean) {
        viewModelScope.launch { datastorePreferences.setCastEnabled(isEnabled) }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { datastorePreferences.toggleFavorite(channel) }
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _currentStreamUrl.value = null
        viewModelScope.launch {
            // Record in recently-watched alongside the stream fetch. Done in
            // parallel because there's no dependency between them.
            launch { datastorePreferences.pushRecentlyWatched(channel) }
            _currentStreamUrl.value = getStreamUrlUseCase(channel.encodedUrl)
        }
    }

    fun setPlayerMode(mode: PlayerMode) { _playerMode.value = mode }

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

    private companion object {
        const val CATEGORY_ALL = "All"
        const val FEATURED_LIMIT = 6
    }
}

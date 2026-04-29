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
import kotlinx.coroutines.launch

class MainViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getStreamUrlUseCase: GetStreamUrlUseCase
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val filteredChannels: StateFlow<List<Channel>> = combine(
        _channels,
        _searchQuery,
        _selectedCategory
    ) { channels, query, category ->
        channels.filter { channel ->
            val matchesQuery = channel.name.contains(query, ignoreCase = true)
            val matchesCategory = if (category == "All") {
                true
            } else {
                // simple mock category matching based on channel name
                when (category) {
                    "News" -> channel.name.contains("news", true) || channel.name.contains("samachar", true) || channel.name.contains("tv", true)
                    "Movies" -> channel.name.contains("movie", true) || channel.name.contains("cinema", true) || channel.name.contains("max", true)
                    "Sports" -> channel.name.contains("sport", true) || channel.name.contains("action", true) || channel.name.contains("ten", true) || channel.name.contains("star", true)
                    "Kids" -> channel.name.contains("kid", true) || channel.name.contains("cartoon", true) || channel.name.contains("disney", true) || channel.name.contains("nick", true) || channel.name.contains("pogo", true) || channel.name.contains("hungama", true)
                    "Music" -> channel.name.contains("music", true) || channel.name.contains("mtv", true) || channel.name.contains("vh1", true)
                    "Gaming" -> channel.name.contains("game", true) || channel.name.contains("gaming", true) || channel.name.contains("play", true)
                    else -> true
                }
            }
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentStreamUrl = MutableStateFlow<String?>(null)
    val currentStreamUrl: StateFlow<String?> = _currentStreamUrl

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val categories = listOf("All", "News", "Movies", "Sports", "Kids", "Music", "Gaming")

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetchedChannels = getChannelsUseCase()
            _channels.value = fetchedChannels
            
            // Default to playing the first channel
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

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _currentStreamUrl.value = null // reset url while fetching new
        viewModelScope.launch {
            val url = getStreamUrlUseCase(channel.id)
            _currentStreamUrl.value = url
        }
    }
    
    fun closePlayer() {
        _selectedChannel.value = null
        _currentStreamUrl.value = null
    }
}

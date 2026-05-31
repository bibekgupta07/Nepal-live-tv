package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.analytics.Telemetry
import com.app.nepallivetv.domain.model.HomeFeed
import com.app.nepallivetv.domain.model.MediaDetail
import com.app.nepallivetv.domain.model.MediaItem
import com.app.nepallivetv.domain.model.MediaKind
import com.app.nepallivetv.domain.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoviesViewModel(
    private val repo: MediaRepository,
    private val telemetry: Telemetry,
) : ViewModel() {

    sealed interface HomeState {
        data object Loading : HomeState
        data class Ready(val feed: HomeFeed) : HomeState
        data class Failed(val message: String) : HomeState
    }

    sealed interface DetailState {
        data object Loading : DetailState
        data class Ready(val detail: MediaDetail) : DetailState
        data class Failed(val message: String) : DetailState
    }

    private val _home = MutableStateFlow<HomeState>(HomeState.Loading)
    val home: StateFlow<HomeState> = _home.asStateFlow()

    private val _detail = MutableStateFlow<DetailState>(DetailState.Loading)
    val detail: StateFlow<DetailState> = _detail.asStateFlow()

    private val _search = MutableStateFlow<List<MediaItem>>(emptyList())
    val search: StateFlow<List<MediaItem>> = _search.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHome()
    }

    fun loadHome() {
        _home.value = HomeState.Loading
        viewModelScope.launch {
            val feed = repo.getHome()
            _home.value = if (feed == null || (feed.hero.isEmpty() && feed.rows.isEmpty())) {
                HomeState.Failed("Couldn't reach the movie catalog. Pull to retry.")
            } else {
                HomeState.Ready(feed)
            }
        }
    }

    fun loadDetail(kind: MediaKind, id: String) {
        _detail.value = DetailState.Loading
        viewModelScope.launch {
            val detail = repo.getDetail(kind, id)
            _detail.value = if (detail == null) {
                DetailState.Failed("Couldn't load details. Try again.")
            } else {
                DetailState.Ready(detail)
            }
        }
    }

    /** Debounced. Each call cancels the previous in-flight request. */
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            val trimmed = query.trim()
            val results = repo.search(trimmed)
            _search.value = results
            telemetry.searchPerformed(queryLength = trimmed.length, resultCount = results.size)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = emptyList()
    }
}

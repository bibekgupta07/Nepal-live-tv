package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.model.Movie
import com.app.nepallivetv.domain.usecase.GetMoviesUseCase
import com.app.nepallivetv.domain.usecase.SearchMoviesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoviesViewModel(
    private val getMoviesUseCase: GetMoviesUseCase,
    private val searchMoviesUseCase: SearchMoviesUseCase
) : ViewModel() {

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentOffset = 0
    private val limit = 50 // Increased limit to fetch more results per page, crucial for searches returning series folders
    private var isLastPage = false

    init {
        loadMovies()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        refresh() // Refresh list entirely when query changes
    }

    fun loadMovies() {
        if (_isLoading.value || isLastPage) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currentQuery = _searchQuery.value.trim()
                val newMovies = if (currentQuery.isEmpty()) {
                    getMoviesUseCase(limit, currentOffset)
                } else {
                    searchMoviesUseCase(currentQuery, limit, currentOffset)
                }

                if (newMovies.isEmpty()) {
                    isLastPage = true
                } else {
                    val currentList = _movies.value.toMutableList()
                    currentList.addAll(newMovies)
                    _movies.value = currentList
                    currentOffset += limit
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load movies"
            } finally {
                _isLoading.value = false
            }
        }
    }

    
    fun refresh() {
        currentOffset = 0
        isLastPage = false
        _movies.value = emptyList()
        loadMovies()
    }
}

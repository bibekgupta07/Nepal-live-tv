package com.app.nepallivetv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.data.model.MatchDetail
import com.app.nepallivetv.domain.usecase.GetMatchDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class MatchDetailViewModel(
    private val getMatchDetailUseCase: GetMatchDetailUseCase
) : ViewModel() {

    private val _matchDetail = MutableStateFlow<MatchDetail?>(null)
    val matchDetail: StateFlow<MatchDetail?> = _matchDetail

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var pollingJob: Job? = null

    fun fetchDetails(matchId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _isLoading.value = true
            while (isActive) {
                try {
                    val detail = getMatchDetailUseCase(matchId)
                    _matchDetail.value = detail
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isLoading.value = false
                }
                delay(10000) // Poll every 10 seconds for live score updates
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
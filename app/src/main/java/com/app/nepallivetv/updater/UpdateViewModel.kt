package com.app.nepallivetv.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(private val updateManager: UpdateManager) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateManager.UpdateResult>(UpdateManager.UpdateResult.NoUpdate)
    val updateState: StateFlow<UpdateManager.UpdateResult> = _updateState.asStateFlow()

    init {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = updateManager.checkForUpdates()
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateManager.UpdateResult.NoUpdate
    }

    fun downloadAndInstall(url: String) {
        updateManager.downloadAndInstallUpdate(url)
        dismissUpdate()
    }
}

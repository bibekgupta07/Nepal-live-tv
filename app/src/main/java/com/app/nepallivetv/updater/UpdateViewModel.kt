package com.app.nepallivetv.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nepallivetv.BuildConfig
import com.app.nepallivetv.analytics.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val updateManager: UpdateManager,
    private val telemetry: Telemetry,
) : ViewModel() {

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
        val toVersion = (_updateState.value as? UpdateManager.UpdateResult.UpdateAvailable)
            ?.version ?: "unknown"
        telemetry.updateAccepted(
            fromVersion = BuildConfig.VERSION_NAME,
            toVersion = toVersion,
        )
        updateManager.downloadAndInstallUpdate(url)
        dismissUpdate()
    }
}

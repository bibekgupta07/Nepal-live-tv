package com.app.nepallivetv.presentation.screens.movies

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.nepallivetv.BuildConfig
import com.app.nepallivetv.presentation.components.VideoPlayer

@Composable
fun MoviePlayerScreen(
    title: String,
    streamUrl: String,
    onBack: () -> Unit
) {
    val fullStreamUrl = if (streamUrl.startsWith("/")) {
        "${BuildConfig.BASE_URL.removeSuffix("/")}$streamUrl"
    } else streamUrl

    VideoPlayer(
        modifier = Modifier.fillMaxSize(),
        streamUrl = fullStreamUrl,
        channelName = title,
        isFullScreen = true,
        isCastEnabled = true, // Enables Chromecast!
        onToggleFullScreen = { onBack() },
        onClose = { onBack() }
    )
}

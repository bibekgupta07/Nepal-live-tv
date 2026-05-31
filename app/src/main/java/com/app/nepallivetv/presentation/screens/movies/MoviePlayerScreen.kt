package com.app.nepallivetv.presentation.screens.movies

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.app.nepallivetv.analytics.Telemetry
import com.app.nepallivetv.domain.repository.MediaRepository
import com.app.nepallivetv.ui.theme.BrandRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// Native player for a movie or episode: fetches m3u8 on entry, hands it to
// ExoPlayer, forces landscape, and keeps the screen on while playing.
@Composable
fun MoviePlayerScreen(
    title: String,
    kind: String,            // "movie" or "show"
    slug: String,
    idEpisode: Long?,         // null for movies
    onBack: () -> Unit,
) {
    val vm: MoviePlayerViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(kind, slug, idEpisode) {
        vm.load(kind, slug, idEpisode)
    }

    BackHandler(onBack = onBack)

    // Force landscape while playing (sensor-landscape lets the user flip between
    // landscape-left and landscape-right, but never portrait). Restore the prior
    // orientation when the screen leaves the composition so the rest of the app
    // stays portrait.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (originalOrientation != null) {
                activity?.requestedOrientation = originalOrientation
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val s = state) {
            is MoviePlayerViewModel.PlayerState.Loading -> CenteredLoading("Loading stream…")
            is MoviePlayerViewModel.PlayerState.Failed -> CenteredFailure(
                message = s.message,
                onRetry = { vm.load(kind, slug, idEpisode) },
                onBack = onBack,
            )
            is MoviePlayerViewModel.PlayerState.Ready -> NativePlayer(
                url = s.url,
                title = title,
            )
        }
        // Always-visible back button overlay so the user is never trapped in
        // the player. Sits over the PlayerView's own controls in the top-left.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 12.dp, top = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xAA000000)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun NativePlayer(url: String, title: String) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                controllerAutoShow = true
                controllerShowTimeoutMs = 2500
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setBackgroundColor(android.graphics.Color.BLACK)
                setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY,
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CenteredLoading(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandRed, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CenteredFailure(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "Can't play this title",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Retry", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Go back", color = Color.White) }
        }
    }
}

/**
 * View model owned by the player screen. Holds the m3u8 fetch state. Kept
 * separate from [com.app.nepallivetv.presentation.viewmodel.MoviesViewModel]
 * so it doesn't survive past this screen and accidentally serve a stale URL.
 */
class MoviePlayerViewModel(
    private val repo: MediaRepository,
    private val telemetry: Telemetry,
) : ViewModel() {

    sealed interface PlayerState {
        data object Loading : PlayerState
        data class Ready(val url: String) : PlayerState
        data class Failed(val message: String) : PlayerState
    }

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Loading)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    fun load(kind: String, slug: String, idEpisode: Long?) {
        _state.value = PlayerState.Loading
        viewModelScope.launch {
            val streams = when (kind) {
                "movie" -> repo.getMovieStream(slug)
                "show" -> if (idEpisode != null) repo.getEpisodeStream(slug, idEpisode) else null
                else -> null
            }
            when {
                streams == null -> {
                    _state.value = PlayerState.Failed(
                        "Couldn't reach the stream provider. Try again in a moment.",
                    )
                    telemetry.mediaPlayFailed(kind, reason = "scraper_error")
                }
                !streams.hasAny -> {
                    _state.value = PlayerState.Failed(
                        "No playable quality available for this title.",
                    )
                    telemetry.mediaPlayFailed(kind, reason = "no_stream")
                }
                else -> {
                    _state.value = PlayerState.Ready(streams.best)
                    telemetry.mediaPlayStarted(kind, title = slug)
                }
            }
        }
    }
}

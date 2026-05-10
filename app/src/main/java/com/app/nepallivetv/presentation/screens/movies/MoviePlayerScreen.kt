package com.app.nepallivetv.presentation.screens.movies

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.app.nepallivetv.BuildConfig
import kotlinx.coroutines.delay
import kotlin.math.max

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MoviePlayerScreen(title: String, streamUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val fullStreamUrl = if (streamUrl.startsWith("/")) "${BuildConfig.BASE_URL.removeSuffix("/")}$streamUrl" else streamUrl

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        // Here is the best caching algorithm for VOD!
        // We tell ExoPlayer to aggressively cache up to 30 minutes (1.8 million ms)
        // of the video directly into the phone's memory if the WiFi is fast!
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000,          // Minimum buffer 50 seconds before playback starts
                1000 * 60 * 30, // Max buffer 30 MINUTES (allows aggressive WiFi caching)
                2500,
                5000
            ).build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(5000)    // Exact 5 seconds rewind
            .setSeekForwardIncrementMs(5000) // Exact 5 seconds fast-forward
            .build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(fullStreamUrl)))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingState: Boolean) {
                        isPlaying = isPlayingState
                    }
                })
            }
    }

    // Timer to auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    // Update progress bar continuously
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = max(exoPlayer.duration, 0L)
            delay(1000)
        }
    }

    // Force landscape mode
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
            exoPlayer.release()
        }
    }

    // Handle app backgrounding safely
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
            else if (event == Lifecycle.Event.ON_RESUME) exoPlayer.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { showControls = !showControls }) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We are using our own custom UI below!
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Overlay UI
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
                
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp).align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
                }

                // Center Controls (Rewind, Play, Forward)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exoPlayer.seekBack() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Default.FastRewind, contentDescription = "-5s", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Box(
                        modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape).clickable {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(onClick = { exoPlayer.seekForward() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Default.FastForward, contentDescription = "+5s", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }

                // Bottom Timeline and Slider
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 48.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = formatTime(currentPosition), color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Slider(
                        value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f,
                        onValueChange = { percent ->
                            val newPos = (percent * duration).toLong()
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary, 
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = formatTime(duration), color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = (totalSeconds / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
package com.app.nepallivetv.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    streamUrl: String?,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isMuted by remember { mutableStateOf(false) }

    var indicatorValue by remember { mutableFloatStateOf(0f) }
    var isVolumeIndicator by remember { mutableStateOf(true) }
    var showIndicator by remember { mutableStateOf(false) }

    // Manage Window states based on isFullScreen
    DisposableEffect(isFullScreen) {
        val window = activity?.window
        var insetsController: WindowInsetsControllerCompat? = null
        if (window != null) {
            insetsController = WindowCompat.getInsetsController(window, window.decorView)
        }

        if (isFullScreen) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        
        onDispose {
            // Nothing to tear down per-state change, proper destroy handled by the unit DisposableEffect below
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl != null && exoPlayer == null) {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
                        "Accept" to "*/*",
                        "Connection" to "keep-alive"
                    )
                )
            
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build().apply {
                playWhenReady = true
            }
        }
        
        if (streamUrl != null) {
            val mediaItem = MediaItem.fromUri(streamUrl)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        }
    }

    // Auto-hide volume/brightness indicator after 1.5 seconds
    LaunchedEffect(indicatorValue, showIndicator) {
        if (showIndicator) {
            delay(1500)
            showIndicator = false
        }
    }

    if (streamUrl != null) {
        Box(modifier = modifier.background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = resizeMode
                    exoPlayer?.volume = if (isMuted) 0f else 1f
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        var startX = 0f
                        var startY = 0f
                        var isVolumeSwipe = false
                        var isBrightnessSwipe = false
                        var startVolume = 0
                        var startBrightness = 0f

                        detectDragGestures(
                            onDragStart = { offset ->
                                startX = offset.x
                                startY = offset.y
                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                startBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                if (startBrightness < 0) startBrightness = 0.5f
                                
                                val halfScreenWidth = size.width / 2
                                if (startX > halfScreenWidth) {
                                    isVolumeSwipe = true
                                    isBrightnessSwipe = false
                                } else {
                                    isBrightnessSwipe = true
                                    isVolumeSwipe = false
                                }
                            },
                            onDragEnd = {
                                isVolumeSwipe = false
                                isBrightnessSwipe = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaY = startY - change.position.y // Up is positive
                                val percent = deltaY / size.height

                                if (abs(deltaY) > 50) { // Threshold
                                    if (isVolumeSwipe) {
                                        val volChange = (percent * maxVolume).toInt()
                                        val newVolume = (startVolume + volChange).coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                        indicatorValue = newVolume.toFloat() / maxVolume
                                        isVolumeIndicator = true
                                        showIndicator = true
                                        if (newVolume > 0 && isMuted) isMuted = false
                                    } else if (isBrightnessSwipe) {
                                        val brightChange = percent * 1.5f
                                        val newBrightness = (startBrightness + brightChange).coerceIn(0.01f, 1f)
                                        activity?.window?.let { win ->
                                            val lp = win.attributes
                                            lp.screenBrightness = newBrightness
                                            win.attributes = lp
                                        }
                                        indicatorValue = newBrightness
                                        isVolumeIndicator = false
                                        showIndicator = true
                                    }
                                }
                            }
                        )
                    }
            )

            // Top Left: Back/Close button
            PlayerIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close",
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            // Top Right: Controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp), // Avoids notching
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerIconButton(
                    icon = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Filled.FitScreen
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> Icons.Filled.Fullscreen
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Icons.Filled.ZoomIn
                        else -> Icons.Filled.FitScreen
                    },
                    contentDescription = "Resize Mode",
                    onClick = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                )
                
                PlayerIconButton(
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute Toggle",
                    onClick = { isMuted = !isMuted }
                )
                
                PlayerIconButton(
                    icon = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    onClick = onToggleFullScreen
                )
            }

            // Volume / Brightness Indicator
            AnimatedVisibility(
                visible = showIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isVolumeIndicator) "VOLUME" else "BRIGHTNESS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { indicatorValue },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier
                                .width(150.dp)
                                .height(8.dp),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    contentDescription: String, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

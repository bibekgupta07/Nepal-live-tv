package com.app.nepallivetv.presentation.components

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.abs



/**
 * A highly customized, gesture-enabled Jetpack Compose wrapper for ExoPlayer.
 * Supports Fullscreen toggling, Picture-in-Picture mode, live-stream optimizations,
 * and swipe gestures for volume and brightness.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    streamUrl: String?,
    channelName: String = "Live Stream",
    isFullScreen: Boolean,
    isInPipMode: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // --- PLAYER STATE ---
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isMuted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isControlsVisible by remember { mutableStateOf(false) }

    // --- GESTURE / OVERLAY STATE ---
    var indicatorValue by remember { mutableFloatStateOf(0f) }
    var isVolumeIndicator by remember { mutableStateOf(true) }
    var showIndicator by remember { mutableStateOf(false) }

    // =======================================================
    // 1. WINDOW SYSTEM & LIFECYCLE MANAGEMENT
    // =======================================================

    // Controls showing/hiding the Android status bar and rotating the physical screen
    DisposableEffect(isFullScreen) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        if (isFullScreen) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.hide(WindowInsetsCompat.Type.systemBars()) // Hide clock & battery
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE // Force Horizontal
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars()) // Show clock
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED // Let sensor decide
        }
        onDispose { }
    }

    // Listens to Android App Lifecycle (Background/Foreground)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // If the user goes to the home screen (and NOT in Picture-in-Picture mode)
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (activity != null && !activity.isInPictureInPictureMode) {
                    exoPlayer?.pause() // Stop pulling network data and stop audio
                }
            } 
            // If the user opens the app back up
            else if (event == Lifecycle.Event.ON_RESUME) {
                exoPlayer?.play() // Resume stream
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Instantly hide the playback controls if Android shrinks the app into PiP mode
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) isControlsVisible = false
    }

    // Cleanup: Make sure we destroy the ExoPlayer completely when this View is closed
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

    // =======================================================
    // 2. EXOPLAYER INITIALIZATION & LIVE OPTIMIZATION
    // =======================================================
    
    LaunchedEffect(streamUrl) {
        // Build the player engine
        if (streamUrl != null && exoPlayer == null) {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
                        "Accept" to "*/*",
                        "Connection" to "keep-alive"
                    )
                )
            
            // Custom Load Control: Don't wait to download 10 seconds of video before starting. 
            // Start as soon as 1.5 seconds is buffered.
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    1500,  // min buffer before playback starts
                    10000, // max buffer allowed to download
                    1000,  // buffer required to resume after freezing
                    1500
                )
                .build()

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .build()
                
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        errorMessage = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    errorMessage = "Playback failed. Please try again."
                    isBuffering = false
                }
            })
            
            player.playWhenReady = true
            exoPlayer = player
        }
        
        // Feed the URL to the player engine
        if (streamUrl != null) {
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        // Automatically slow down video by 4% if network is struggling (prevents spinning wheel)
                        .setMinPlaybackSpeed(0.96f)
                        // Automatically speed up video by 4% if we are lagging behind the live edge
                        .setMaxPlaybackSpeed(1.04f)
                        // Target playing 3 seconds behind "live" to allow a tiny buffer
                        .setTargetOffsetMs(3000)
                        .setMinOffsetMs(1500)
                        .setMaxOffsetMs(15000)
                        .build()
                )
                .build()

            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        }
    }

    // Timer to automatically hide custom UI Controls after 3 seconds of inactivity
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // Timer to automatically hide the Brightness/Volume graphic after swiping
    LaunchedEffect(indicatorValue, showIndicator) {
        if (showIndicator) {
            delay(1500)
            showIndicator = false
        }
    }

    // =======================================================
    // 3. UI RENDERING
    // =======================================================

    if (streamUrl != null) {
        Box(modifier = modifier.background(Color.Black)) {
            
            // The actual raw video feed surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // We use our own custom compose overlay, completely hiding ExoPlayer's ugly native controls
                        useController = false
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = resizeMode
                    exoPlayer?.volume = if (isMuted) 0f else 1f
                },
                modifier = Modifier
                    .fillMaxSize()
                    .videoPlayerGestures(
                        context = context,
                        activity = activity,
                        isInPipMode = isInPipMode,
                        onTap = { isControlsVisible = !isControlsVisible },
                        onSwipe = { value, isVolume ->
                            indicatorValue = value
                            isVolumeIndicator = isVolume
                            showIndicator = true
                            if (isVolume && value > 0 && isMuted) isMuted = false
                        }
                    )
            )

            // --- UI OVERLAYS ---
            
            // Gradient Scrim overlay to make top/bottom text readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Top Left: LIVE Badge
            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(if (!isFullScreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                }
            }

            // Top Right: Favorite Button, Close Button & Channel Name
            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(if (!isFullScreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerIconButton(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        onClick = onToggleFavorite
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onClose() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color(0xFF6B8AFF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = channelName.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Center Controls (Play/Pause wrapper mock)
            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(6.dp))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE63946), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(6.dp))
                }
            }

            // Bottom Left: Show Title & Progress Bar
            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp, end = 120.dp)
            ) {
                Column {
                    Text(text = channelName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Live Now • HD", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 1f }, // Mock full progress for live
                        color = Color(0xFFE63946),
                        trackColor = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Bottom Right: System Controls (Rotated/Mute)
            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                        icon = Icons.Filled.ScreenRotation,
                        contentDescription = "Toggle Orientation",
                        onClick = onToggleFullScreen
                    )
                }
            }

            // Center Overlay: Volume / Brightness Level Box
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

            // Center Overlay: Loading Spinner
            if (isBuffering && errorMessage == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Center Overlay: Error Panel
            if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        errorMessage = null
                        isBuffering = true
                        exoPlayer?.prepare()
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

/**
 * Clean reusable button layout for the Video Player overlays.
 */
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
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Extension function handling the complex Touch gestures for Brightness (Left Swipe) and Volume (Right Swipe).
 * Extracts this messy logic out of the main compose tree.
 */
private fun Modifier.videoPlayerGestures(
    context: Context,
    activity: Activity?,
    isInPipMode: Boolean,
    onTap: () -> Unit,
    onSwipe: (value: Float, isVolume: Boolean) -> Unit
): Modifier = composed {
    if (isInPipMode) return@composed this // Disable gestures completely if minimized

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    
    var startX = 0f
    var startY = 0f
    var isVolumeSwipe = false
    var isBrightnessSwipe = false
    var startVolume = 0
    var startBrightness = 0f

    this
        .pointerInput(Unit) {
            detectTapGestures(onTap = { onTap() })
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    startX = offset.x
                    startY = offset.y
                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    startBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                    if (startBrightness < 0) startBrightness = 0.5f
                    
                    // Determine if the user touched the left half (brightness) or right half (volume)
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

                    if (abs(deltaY) > 50) { // Drag Threshold
                        if (isVolumeSwipe) {
                            val volChange = (percent * maxVolume).toInt()
                            val newVolume = (startVolume + volChange).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                            
                            val value = newVolume.toFloat() / maxVolume
                            onSwipe(value, true)
                        } else if (isBrightnessSwipe) {
                            val brightChange = percent * 1.5f
                            val newBrightness = (startBrightness + brightChange).coerceIn(0.01f, 1f)
                            activity?.window?.let { win ->
                                val lp = win.attributes
                                lp.screenBrightness = newBrightness
                                win.attributes = lp
                            }
                            
                            onSwipe(newBrightness, false)
                        }
                    }
                }
            )
        }
}
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

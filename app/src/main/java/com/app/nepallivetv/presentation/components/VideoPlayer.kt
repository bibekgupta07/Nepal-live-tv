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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.*
import com.app.nepallivetv.presentation.components.LiveBadge
import com.app.nepallivetv.data.model.Channel
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.delay
import kotlin.math.abs

import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.compose.material.icons.filled.Settings
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

data class VideoQualityOption(
    val resolutionText: String,
    val trackGroup: TrackGroup? = null,
    val trackIndex: Int = -1,
    val isAuto: Boolean = false
)

/**
 * A gesture-enabled Jetpack Compose wrapper for ExoPlayer.
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
    isCastEnabled: Boolean = true,
    channels: List<Channel> = emptyList(),
    selectedChannel: Channel? = null,
    onChannelSelected: (Channel) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var castPlayer by remember { mutableStateOf<CastPlayer?>(null) }
    var isCasting by remember { mutableStateOf(false) }

    var isChannelDrawerOpen by remember { mutableStateOf(false) }

    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isMuted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isControlsVisible by remember { mutableStateOf(false) }

    var indicatorValue by remember { mutableFloatStateOf(0f) }
    var isVolumeIndicator by remember { mutableStateOf(true) }
    var showIndicator by remember { mutableStateOf(false) }

    var isQualityMenuExpanded by remember { mutableStateOf(false) }
    var videoQualities by remember { mutableStateOf(emptyList<VideoQualityOption>()) }
    
    val trackSelector = remember { DefaultTrackSelector(context, AdaptiveTrackSelection.Factory()) }

    var isPlaying by remember { mutableStateOf(true) }

    DisposableEffect(isFullScreen) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

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
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (activity != null && !activity.isInPictureInPictureMode) {
                        exoPlayer?.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    // Always pause on stop to prevent background audio bleeding
                    // (e.g. when app is fully hidden or PiP is dismissed by user)
                    exoPlayer?.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer?.play()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer?.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isInPipMode) {
        if (isInPipMode) isControlsVisible = false
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            castPlayer?.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(Unit) {
        try {
            if (!isCastEnabled) return@LaunchedEffect
            val castContext = CastContext.getSharedInstance(context)
            val player = CastPlayer(castContext)
            player.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                override fun onCastSessionAvailable() {
                    isCasting = true
                    val currentPosition = Math.max(0L, exoPlayer?.currentPosition ?: 0L)
                    exoPlayer?.pause()
                    
                    streamUrl?.let { url ->
                        val mimeType = if (url.contains(".m3u8")) "application/x-mpegURL" else "video/mp4"
                        val mediaItem = MediaItem.Builder()
                            .setUri(url)
                            .setMimeType(mimeType)
                            .build()
                        player.setMediaItem(mediaItem, currentPosition)
                        player.prepare()
                        player.play()
                    }
                }

                override fun onCastSessionUnavailable() {
                    isCasting = false
                    player.stop()
                    exoPlayer?.play()
                }
            })
            castPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(streamUrl) {
        if (exoPlayer == null) {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
                        "Accept" to "*/*",
                        "Connection" to "keep-alive"
                    )
                )
            
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    1500,
                    10000,
                    1000,
                    1500
                )
                .build()

            // Configure to proactively downgrade quality if network is poor
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setMaxVideoSizeSd() // Start with SD to ensure fast playback, it will adapt up if network is good
                .build()

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build()
                
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingState: Boolean) {
                    isPlaying = isPlayingState
                }

                override fun onTracksChanged(tracks: Tracks) {
                    val options = mutableListOf<VideoQualityOption>()
                    options.add(VideoQualityOption("Auto", isAuto = true))
                    
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val height = format.height
                                if (height > 0) {
                                    options.add(VideoQualityOption("${height}p", group.mediaTrackGroup, i))
                                }
                            }
                        }
                    }
                    // Sort descending (e.g. 1080p, 720p, 480p)
                    val uniqueOptions = options.distinctBy { it.resolutionText }.sortedByDescending { it.resolutionText.replace("p", "").toIntOrNull() ?: 9999 }
                    videoQualities = uniqueOptions
                }

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
        
        if (streamUrl != null) {
            isBuffering = true
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setMinPlaybackSpeed(0.96f)
                        .setMaxPlaybackSpeed(1.04f)
                        .setTargetOffsetMs(3000)
                        .setMinOffsetMs(1500)
                        .setMaxOffsetMs(15000)
                        .build()
                )
                .build()

            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        } else {
            // Unload player visually, show loading indicator
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            isBuffering = true
        }
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    LaunchedEffect(indicatorValue, showIndicator) {
        if (showIndicator) {
            delay(1500)
            showIndicator = false
        }
    }

    val controlButtonSize = if (isFullScreen) 48.dp else 32.dp
    val controlIconSize = if (isFullScreen) 24.dp else 16.dp
    val topButtonSize = if (isFullScreen) 40.dp else 28.dp
    val topIconSize = if (isFullScreen) 20.dp else 14.dp
    val centerPlayBgSize = if (isFullScreen) 64.dp else 48.dp
    val centerPlayIconSize = if (isFullScreen) 32.dp else 24.dp

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
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

            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = channelName.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerIconButton(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(topButtonSize),
                        iconSize = topIconSize
                    )

                    PlayerIconButton(
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Channel List",
                        onClick = { isChannelDrawerOpen = !isChannelDrawerOpen },
                        modifier = Modifier.size(topButtonSize),
                        iconSize = topIconSize
                    )

                    if (isCastEnabled) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val wrapper = androidx.appcompat.view.ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)
                                    MediaRouteButton(wrapper).apply {
                                        CastButtonFactory.setUpMediaRouteButton(wrapper, this)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isControlsVisible && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(centerPlayBgSize)
                        .background(Color(0xFFE63946), CircleShape)
                        .clickable {
                            if (isPlaying) {
                                exoPlayer?.pause()
                            } else {
                                exoPlayer?.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(centerPlayIconSize)
                    )
                }
            }

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
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> Icons.Filled.ZoomIn
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Icons.Filled.Crop
                            else -> Icons.Filled.FitScreen
                        },
                        contentDescription = "Resize Mode",
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.size(controlButtonSize),
                        iconSize = controlIconSize
                    )
                    
                    PlayerIconButton(
                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute Toggle",
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(controlButtonSize),
                        iconSize = controlIconSize
                    )
                    
                    Box {
                        PlayerIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Video Quality",
                            onClick = { isQualityMenuExpanded = true },
                            modifier = Modifier.size(controlButtonSize),
                            iconSize = controlIconSize
                        )

                        DropdownMenu(
                            expanded = isQualityMenuExpanded,
                            onDismissRequest = { isQualityMenuExpanded = false },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
                        ) {
                            videoQualities.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option.resolutionText, color = Color.White) },
                                    onClick = {
                                        isQualityMenuExpanded = false
                                        if (option.isAuto) {
                                            trackSelector.parameters = trackSelector.buildUponParameters()
                                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                                .build()
                                        } else {
                                            option.trackGroup?.let { group ->
                                                trackSelector.parameters = trackSelector.buildUponParameters()
                                                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                                    .addOverride(TrackSelectionOverride(group, listOf(option.trackIndex)))
                                                    .build()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    PlayerIconButton(
                        icon = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Toggle Orientation",
                        onClick = onToggleFullScreen,
                        modifier = Modifier.size(controlButtonSize),
                        iconSize = controlIconSize
                    )
                }
            }

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

            AnimatedVisibility(
                visible = isCasting,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(32.dp)
                ) {
                    Icon(Icons.Default.CastConnected, contentDescription = "Casting", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Casting to TV", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (isBuffering && errorMessage == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

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

            AnimatedVisibility(
                visible = isChannelDrawerOpen && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().clickable { isChannelDrawerOpen = false })
            }

            AnimatedVisibility(
                visible = isChannelDrawerOpen && !isInPipMode,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isFullScreen) 0.4f else 0.7f)
                        .background(Color.Black.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Channels", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color.White, modifier = Modifier.clickable { isChannelDrawerOpen = false })
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(channels) { channel ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onChannelSelected(channel)
                                            isChannelDrawerOpen = false 
                                        }
                                        .background(if (channel == selectedChannel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!channel.logo.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = channel.logo,
                                            contentDescription = channel.name,
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column {
                                        Text(text = channel.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                        Text(text = channel.category, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
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
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp
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
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun Modifier.videoPlayerGestures(
    context: Context,
    activity: Activity?,
    isInPipMode: Boolean,
    onTap: () -> Unit,
    onSwipe: (value: Float, isVolume: Boolean) -> Unit
): Modifier = composed {
    if (isInPipMode) return@composed this

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
                    val deltaY = startY - change.position.y
                    val percent = deltaY / size.height

                    if (abs(deltaY) > 50) {
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


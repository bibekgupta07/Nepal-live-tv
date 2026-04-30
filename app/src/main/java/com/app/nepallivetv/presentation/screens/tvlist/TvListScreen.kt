package com.app.nepallivetv.presentation.screens.tvlist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.presentation.components.VideoPlayer
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.app.nepallivetv.presentation.SharedViewModel

// ==============================================================================
// SCREEN
// ==============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvListScreen(
    viewModel: SharedViewModel,
    isInPipMode: Boolean = false,
    bottomPadding: Dp = 0.dp
) {
    val channels by viewModel.tvListChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()

    var isFullScreen by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    var backPressedTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            val isCurrentChannelInList = channels.any { it == selectedChannel }
            if (!isCurrentChannelInList) {
                viewModel.selectNplLive()
            }
        }
    }

    BackHandler(enabled = isFullScreen && !isInPipMode) {
        isFullScreen = false
    }

    BackHandler(enabled = !isFullScreen && !isInPipMode) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            activity?.finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // --- TOP VIDEO PLAYER ---
        if (currentStreamUrl != null) {
            val favoriteUrls by viewModel.favoriteUrls.collectAsState()
            val isCurrentFavorite = selectedChannel?.encodedUrl in favoriteUrls

            VideoPlayer(
                streamUrl = currentStreamUrl,
                channelName = selectedChannel?.name ?: "Live Stream",
                isFullScreen = isFullScreen,
                isInPipMode = isInPipMode,
                isFavorite = isCurrentFavorite,
                onToggleFavorite = { selectedChannel?.let { viewModel.toggleFavorite(it) } },
                onToggleFullScreen = { isFullScreen = !isFullScreen },
                onClose = {
                    if (isFullScreen) {
                        isFullScreen = false
                    } else {
                        viewModel.closePlayer()
                    }
                },
                modifier = if (isFullScreen || isInPipMode) Modifier.fillMaxSize() else Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        // --- MAIN DASHBOARD SCROLLABLE CONTENT ---
        if (!isFullScreen && !isInPipMode) {

            // Grid Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "TV List Channels",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${channels.size} live →",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Grid Content
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(channels) { channel ->
                        ChannelGridItem(
                            channel = channel,
                            isSelected = channel == selectedChannel,
                            onClick = {
                                viewModel.selectChannel(channel)
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }
    } // End Column
} // End TvListScreen

// ==============================================================================
// UI COMPONENTS
// ==============================================================================

@Composable
fun ChannelGridItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                if (!channel.logo.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LiveBadge()
                }
            }
        }
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color.White, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "LIVE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
package com.app.nepallivetv.presentation.screens.mylist

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.presentation.components.VideoPlayer
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    viewModel: SharedViewModel,
    isInPipMode: Boolean = false,
    bottomPadding: Dp = 0.dp
) {
    val channels by viewModel.favoriteChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()

    val isFullScreen by viewModel.isFullScreen.collectAsState()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = isFullScreen && !isInPipMode) {
        viewModel.setFullScreen(false)
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
        if (currentStreamUrl != null) {
            val isCurrentFavorite = selectedChannel?.encodedUrl in favoriteUrls

            VideoPlayer(
                streamUrl = currentStreamUrl,
                channelName = selectedChannel?.name ?: "Live Stream",
                isFullScreen = isFullScreen,
                isInPipMode = isInPipMode,
                isFavorite = isCurrentFavorite,
                onToggleFavorite = { selectedChannel?.let { viewModel.toggleFavorite(it) } },
                onToggleFullScreen = { viewModel.setFullScreen(!isFullScreen) },
                onClose = {
                    if (isFullScreen) {
                        viewModel.setFullScreen(false)
                    } else {
                        viewModel.closePlayer()
                    }
                },
                modifier = if (isFullScreen || isInPipMode) Modifier.fillMaxSize() else Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        if (!isFullScreen && !isInPipMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "My Favorites",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "${channels.size} saved →",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

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
                        text = "You haven't added any favorite channels yet.",
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
                        FavoriteGridItem(
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
    }
}

@Composable
fun FavoriteGridItem(
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
private fun LiveBadge(modifier: Modifier = Modifier) {
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

package com.app.nepallivetv.presentation.screens.mylist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.LocalPipMode
import com.app.nepallivetv.presentation.components.ChannelGridItem
import com.app.nepallivetv.presentation.components.SectionHeader
import com.app.nepallivetv.presentation.components.VideoPlayer
import com.app.nepallivetv.presentation.viewmodel.PlayerMode
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.utils.showToast
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyListScreen() {
    val viewModel = koinViewModel<SharedViewModel>()
    val channels by viewModel.favoriteChannels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val playerMode by viewModel.playerMode.collectAsState()
    val isFullScreen = playerMode == PlayerMode.FULL
    val isInPipMode = LocalPipMode.current
    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()

    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val isCurrentFavorite = selectedChannel?.encodedUrl in favoriteUrls
    val isCastEnabled by viewModel.isCastEnabled.collectAsState()

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isFullScreen && !isInPipMode) {
        viewModel.setFullScreen(false)
    }

    BackHandler(enabled = !isFullScreen && !isInPipMode) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            activity?.finish()
        } else {
            backPressedTime = currentTime
            context.showToast("Press back again to exit")
        }
    }

    val effectiveMode = if (isInPipMode) PlayerMode.FULL else playerMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        if (currentStreamUrl != null || effectiveMode != PlayerMode.MINI) {
            val playerModifier = when (effectiveMode) {
                PlayerMode.FULL -> Modifier.fillMaxSize()
                PlayerMode.EXPANDED -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                PlayerMode.MINI -> Modifier.fillMaxWidth().height(56.dp)
            }
            VideoPlayer(
                streamUrl = currentStreamUrl,
                channelName = selectedChannel?.name ?: "Loading...",
                playerMode = effectiveMode,
                isInPipMode = isInPipMode,
                isFavorite = isCurrentFavorite,
                isCastEnabled = isCastEnabled,
                channels = channels,
                selectedChannel = selectedChannel,
                onChannelSelected = { viewModel.selectChannel(it) },
                onToggleFavorite = { selectedChannel?.let { viewModel.toggleFavorite(it) } },
                onExpand = { viewModel.expandPlayer() },
                onMinimize = { viewModel.minimizePlayer() },
                onToggleFullScreen = { viewModel.setFullScreen(!isFullScreen) },
                onClose = {
                    if (isFullScreen) viewModel.setFullScreen(false) else viewModel.closePlayer()
                },
                modifier = playerModifier
            )
        }

        if (effectiveMode != PlayerMode.FULL) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Saved",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "My List",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                if (channels.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp, bottom = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Nothing here yet",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the heart on any channel to save it for later.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(title = "Favorites", count = channels.size)
                    }
                    items(channels, key = { it.encodedUrl }) { channel ->
                        ChannelGridItem(
                            channel = channel,
                            isSelected = channel == selectedChannel,
                            isFavorite = channel.encodedUrl in favoriteUrls,
                            onClick = {
                                viewModel.selectChannel(channel)
                                focusManager.clearFocus()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(channel) },
                        )
                    }
                }
            }
        }
    }
}

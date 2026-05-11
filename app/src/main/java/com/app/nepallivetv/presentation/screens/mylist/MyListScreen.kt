package com.app.nepallivetv.presentation.screens.mylist

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.LocalPipMode
import com.app.nepallivetv.presentation.components.VideoPlayer
import com.app.nepallivetv.presentation.screens.home.ChannelListItem
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.ui.theme.customColors
import com.app.nepallivetv.utils.showToast
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyListScreen() {
    val viewModel = koinViewModel<SharedViewModel>()
    val channels by viewModel.favoriteChannels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val isInPipMode = LocalPipMode.current
    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        val isPlaying = currentStreamUrl != null

        if (currentStreamUrl != null || isFullScreen || isInPipMode || isLandscape) {
            VideoPlayer(
                streamUrl = currentStreamUrl,
                channelName = selectedChannel?.name ?: "Loading...",
                isFullScreen = isFullScreen,
                isMiniPlayer = isPlaying && !isFullScreen && !isInPipMode && !isLandscape,
                isInPipMode = isInPipMode,
                isFavorite = isCurrentFavorite,
                isCastEnabled = isCastEnabled,
                channels = channels,
                selectedChannel = selectedChannel,
                onChannelSelected = { viewModel.selectChannel(it) },
                onToggleFavorite = { selectedChannel?.let { viewModel.toggleFavorite(it) } },
                onToggleFullScreen = { viewModel.setFullScreen(!isFullScreen) },
                onClose = {
                    if (isFullScreen) {
                        viewModel.setFullScreen(false)
                    } else {
                        viewModel.closePlayer()
                    }
                },
                modifier = if (isFullScreen || isInPipMode || isLandscape) Modifier.fillMaxSize() 
                else Modifier.fillMaxWidth().height(56.dp)
            )
        }

        if (!isFullScreen && !isInPipMode && !isLandscape) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "My List",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "${channels.size} saved",
                        color = MaterialTheme.customColors.settingTextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(channels) { channel ->
                        val isSelected = channel == selectedChannel
                        val isFavorite = channel.encodedUrl in favoriteUrls
                        ChannelListItem(
                            channel = channel,
                            isSelected = isSelected,
                            isFavorite = isFavorite,
                            onToggleFavorite = { viewModel.toggleFavorite(channel) },
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
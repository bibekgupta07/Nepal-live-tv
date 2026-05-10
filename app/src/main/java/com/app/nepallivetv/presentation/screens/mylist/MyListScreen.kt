package com.app.nepallivetv.presentation.screens.mylist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nepallivetv.LocalPipMode
import com.app.nepallivetv.presentation.components.VideoPlayer
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyListScreen() {
    val viewModel = koinViewModel<SharedViewModel>()
    val channels by viewModel.favoriteChannels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
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
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
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
        VideoPlayer(
            streamUrl = currentStreamUrl,
            channelName = selectedChannel?.name ?: "Loading...",
            isFullScreen = isFullScreen,
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
            modifier = if (isFullScreen || isInPipMode) Modifier.fillMaxSize() else Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        if (!isFullScreen && !isInPipMode) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(24.dp))

                Column {
                    Text(
                        text = "Saved channels",
                        color = MaterialTheme.customColors.settingTextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "My List",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "My Favorites",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${channels.size} saved",
                        color = MaterialTheme.customColors.settingTextGray,
                        fontSize = 12.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(channels) { channel ->
                        val isSelected = channel == selectedChannel
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectChannel(channel) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) CardActiveBg else MaterialTheme.customColors.cardInactiveBg),
                            border = if (isSelected) BorderStroke(1.dp, CardActiveBorder) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!channel.logo.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = channel.logo,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                            .padding(4.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                color = if (channel.name.contains("M2", true)) M2GradientStart 
                                                        else if (channel.name.contains("Kantipur", true)) KBlueBg 
                                                        else if (channel.name.contains("AP1", true)) AP1PurpleBg 
                                                        else if (channel.name.contains("Nepal TV", true)) NTVGreenBg 
                                                        else M2GradientStart, 
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = channel.name.take(2).uppercase(),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = channel.category.ifBlank { "Live TV" },
                                        color = MaterialTheme.customColors.settingTextGray,
                                        fontSize = 12.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        modifier = Modifier
                                            .background(BrandRed, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LIVE", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, HdBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("HD", color = MaterialTheme.customColors.settingTextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
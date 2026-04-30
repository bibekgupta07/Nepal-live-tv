package com.app.nepallivetv.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.AsyncImage
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.presentation.MainViewModel

/**
 * MainScreen is the primary dashboard of the application.
 * It intelligently orchestrates the VideoPlayer, the Search/Category row, and the Grid of channels.
 *
 * @param viewModel The ViewModel providing state.
 * @param isInPipMode Boolean indicating if Android is currently minimizing the app into Picture-in-Picture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, isInPipMode: Boolean = false) {
    // Collect all states safely from the ViewModel
    val channels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    // UI Local States
    var isFullScreen by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    
    // Used to handle keyboard and focus events smoothly
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    var backPressedTime by remember { mutableLongStateOf(0L) }

    // Keeps search bar expanded if the user has actively typed something in it
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearchVisible = true
        }
    }

    // --- BACK BUTTON HANDLING ---
    // If we are in fullscreen, back button exits fullscreen
    BackHandler(enabled = isFullScreen && !isInPipMode) {
        isFullScreen = false
    }
    
    // If we are in normal view, require a double-tap on the back button to exit the app completely
    BackHandler(enabled = !isFullScreen && !isInPipMode) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            activity?.finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MAIN UI LAYOUT ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else paddingValues)
                // This globally catches taps outside the keyboard and closes the keyboard!
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        focusManager.clearFocus()
                    }
                }
        ) {
            
            // 1. TOP VIDEO PLAYER SECTION
            if (currentStreamUrl != null) {
                VideoPlayer(
                    streamUrl = currentStreamUrl,
                    isFullScreen = isFullScreen,
                    isInPipMode = isInPipMode, // Passes PiP state down so controls can hide automatically
                    onToggleFullScreen = { isFullScreen = !isFullScreen },
                    onClose = { 
                        if (isFullScreen) {
                            isFullScreen = false
                        } else {
                            viewModel.closePlayer() 
                        }
                    },
                    // If fullscreen or in tiny PiP mode, fill the whole screen. Otherwise, maintain 16:9 ratio.
                    modifier = if (isFullScreen || isInPipMode) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
            }

            // 2. BOTTOM GRID & SEARCH SECTION (Hidden during Fullscreen or PiP)
            if (!isFullScreen && !isInPipMode) {
                
                // Triggers the keyboard to open automatically when the search bar becomes visible
                LaunchedEffect(isSearchVisible) {
                    if (isSearchVisible) {
                        focusRequester.requestFocus()
                    }
                }

                // -> SEARCH BAR & CATEGORY ROW COMPONENT
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .defaultMinSize(minHeight = 52.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isSearchVisible) {
                        // Shows the expanded Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search Channels...") },
                            leadingIcon = { 
                                Icon(Icons.Default.Search, contentDescription = "Search") 
                            },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    // Always clear search, close keyboard, and hide search bar
                                    viewModel.onSearchQueryChanged("")
                                    isSearchVisible = false
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Close/Clear Search")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            singleLine = true
                        )
                    } else {
                        // Shows the Horizontal scrollable Category Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isSearchVisible = true },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = "Open Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(categories) { category ->
                                    FilterChip(
                                        selected = category == selectedCategory,
                                        onClick = { 
                                            viewModel.onCategorySelected(category)
                                            focusManager.clearFocus()
                                        },
                                        label = { Text(category) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // -> CHANNEL GRID COMPONENT
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (channels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No channels found.", 
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp), // Dynamically adapts columns to fit width
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channels) { channel ->
                            ChannelItem(
                                channel = channel,
                                isSelected = channel == selectedChannel,
                                onClick = { 
                                    // Start playing channel, clear search, and close keyboard
                                    viewModel.selectChannel(channel)
                                    viewModel.onSearchQueryChanged("")
                                    isSearchVisible = false
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Represents a single rectangular Channel Card in the grid.
 */
@Composable
fun ChannelItem(channel: Channel, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        // Highlights the active card with the primary theme color
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Inner white box holding the Channel Logo to standardize all transparent/weirdly sized logos
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White) 
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Text Label below the logo
            Text(
                text = channel.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2, // Forces exactly 2 lines so grid rows align perfectly
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

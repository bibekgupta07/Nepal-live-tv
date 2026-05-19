package com.app.nepallivetv.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.LocalPipMode
import com.app.nepallivetv.presentation.components.ChannelCircleItem
import com.app.nepallivetv.presentation.components.ChannelGridItem
import com.app.nepallivetv.presentation.components.HeroCard
import com.app.nepallivetv.presentation.components.HeroCardSkeleton
import com.app.nepallivetv.presentation.components.SectionHeader
import com.app.nepallivetv.presentation.components.VideoPlayer
import com.app.nepallivetv.presentation.viewmodel.PlayerMode
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.utils.showToast
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * Multi-section home feed.
 *
 *   ┌─ Top bar (greeting + search affordance)
 *   ├─ Featured hero pager (HorizontalPager, 1 hero per page)
 *   ├─ Continue Watching strip (only if non-empty)
 *   ├─ Favorites strip          (only if non-empty)
 *   ├─ Category chips           (synthetic categories from CategoryClassifier)
 *   └─ All-channels grid        (LazyVerticalGrid, 3 columns)
 *
 * Layout is one LazyVerticalGrid; non-grid sections occupy
 * GridItemSpan(maxLineSpan) so they look like full-width rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val viewModel = koinViewModel<SharedViewModel>()
    val isInPipMode = LocalPipMode.current

    val channels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentStreamUrl by viewModel.currentStreamUrl.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val favoriteUrls by viewModel.favoriteUrls.collectAsState()
    val favorites by viewModel.favoriteChannels.collectAsState()
    val recents by viewModel.recentlyWatched.collectAsState()
    val featured by viewModel.featuredChannels.collectAsState()
    val isCastEnabled by viewModel.isCastEnabled.collectAsState()
    val userName by viewModel.datastorePreferences.userNameFlow.collectAsState(initial = null)

    val playerMode by viewModel.playerMode.collectAsState()
    val isFullScreen = playerMode == PlayerMode.FULL
    var isSearchVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = isFullScreen && !isInPipMode) {
        viewModel.setFullScreen(false)
    }

    BackHandler(enabled = !isFullScreen && !isInPipMode) {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - backPressedTime) < 2000) {
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
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
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
                isFavorite = selectedChannel?.encodedUrl in favoriteUrls,
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
            FeedBody(
                isLoading = isLoading,
                searchQuery = searchQuery,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                isSearchVisible = isSearchVisible,
                onSearchVisibleChanged = { isSearchVisible = it },
                userName = userName,
                featured = featured,
                recents = recents,
                favorites = favorites,
                channels = channels,
                favoriteUrls = favoriteUrls,
                selectedChannel = selectedChannel,
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.onCategorySelected(it) },
                onChannelClicked = { ch ->
                    viewModel.selectChannel(ch)
                    if (searchQuery.isNotEmpty()) {
                        viewModel.onSearchQueryChanged("")
                        isSearchVisible = false
                    }
                    focusManager.clearFocus()
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedBody(
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isSearchVisible: Boolean,
    onSearchVisibleChanged: (Boolean) -> Unit,
    userName: String?,
    featured: List<com.app.nepallivetv.domain.model.Channel>,
    recents: List<com.app.nepallivetv.domain.model.Channel>,
    favorites: List<com.app.nepallivetv.domain.model.Channel>,
    channels: List<com.app.nepallivetv.domain.model.Channel>,
    favoriteUrls: Set<String>,
    selectedChannel: com.app.nepallivetv.domain.model.Channel?,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onChannelClicked: (com.app.nepallivetv.domain.model.Channel) -> Unit,
    onToggleFavorite: (com.app.nepallivetv.domain.model.Channel) -> Unit,
) {
    val isSearching = searchQuery.isNotEmpty()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        item(span = { GridItemSpan(maxLineSpan) }) {
            TopGreetingBar(
                userName = userName,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                isSearchVisible = isSearchVisible,
                onSearchVisibleChanged = onSearchVisibleChanged,
            )
        }

        if (!isSearching) {
            // ── Hero pager ────────────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (isLoading && featured.isEmpty()) {
                    HeroCardSkeleton(modifier = Modifier.padding(horizontal = 4.dp))
                } else if (featured.isNotEmpty()) {
                    FeaturedPager(
                        items = featured,
                        onClick = onChannelClicked,
                    )
                }
            }

            // ── Continue watching ─────────────────────────────────────────
            if (recents.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(title = "Continue Watching", count = recents.size)
                        ChannelStrip(
                            items = recents,
                            selectedId = selectedChannel?.encodedUrl,
                            onClick = onChannelClicked,
                        )
                    }
                }
            }

            // ── Favorites ─────────────────────────────────────────────────
            if (favorites.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(title = "Your Favorites", count = favorites.size)
                        ChannelStrip(
                            items = favorites,
                            selectedId = selectedChannel?.encodedUrl,
                            onClick = onChannelClicked,
                        )
                    }
                }
            }

            // ── Category chips ───────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryChips(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = onCategorySelected,
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = if (selectedCategory == "All") "All Channels" else selectedCategory,
                    count = channels.size,
                )
            }
        }

        // ── Grid ──────────────────────────────────────────────────────────
        if (isLoading && channels.isEmpty()) {
            items(6) {
                GridSkeleton()
            }
        } else if (channels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    message = if (isSearching) "No channels match \"$searchQuery\""
                    else "No channels available."
                )
            }
        } else {
            items(channels, key = { it.encodedUrl }) { ch ->
                ChannelGridItem(
                    channel = ch,
                    isSelected = ch == selectedChannel,
                    isFavorite = ch.encodedUrl in favoriteUrls,
                    onClick = { onChannelClicked(ch) },
                    onToggleFavorite = { onToggleFavorite(ch) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopGreetingBar(
    userName: String?,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isSearchVisible: Boolean,
    onSearchVisibleChanged: (Boolean) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!userName.isNullOrBlank()) "Hi, $userName" else "Welcome back",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "What's on?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            AnimatedVisibility(visible = !isSearchVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(
                    onClick = { onSearchVisibleChanged(true) },
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = isSearchVisible, enter = fadeIn(), exit = fadeOut()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search channels...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    IconButton(onClick = {
                        onSearchQueryChanged("")
                        onSearchVisibleChanged(false)
                    }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .focusRequester(focusRequester)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturedPager(
    items: List<com.app.nepallivetv.domain.model.Channel>,
    onClick: (com.app.nepallivetv.domain.model.Channel) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-advance every 5s so the carousel feels alive but isn't aggressive.
    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) { page ->
            HeroCard(channel = items[page], onClick = { onClick(items[page]) })
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            items.indices.forEach { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (active) 18.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

@Composable
private fun ChannelStrip(
    items: List<com.app.nepallivetv.domain.model.Channel>,
    selectedId: String?,
    onClick: (com.app.nepallivetv.domain.model.Channel) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items, key = { it.encodedUrl }) { ch ->
            ChannelCircleItem(
                channel = ch,
                isSelected = ch.encodedUrl == selectedId,
                onClick = { onClick(ch) },
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { cat ->
            val isSelected = cat == selected
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelect(cat) }
            ) {
                Text(
                    text = cat,
                    color = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun GridSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

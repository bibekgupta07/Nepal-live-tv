package com.app.nepallivetv.presentation.screens.movies

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.domain.model.MediaItem
import com.app.nepallivetv.presentation.components.MovieHero
import com.app.nepallivetv.presentation.components.MoviePoster
import com.app.nepallivetv.presentation.components.MovieRow
import com.app.nepallivetv.presentation.viewmodel.MoviesViewModel
import com.app.nepallivetv.ui.theme.AccentOrange
import com.app.nepallivetv.ui.theme.BrandRed
import org.koin.androidx.compose.koinViewModel

/**
 * The Netflix-style Movies tab. Owns no playback — taps on items bubble up
 * to [onOpenDetail] which the navigator hands off to [MovieDetailScreen].
 */
@Composable
fun MoviesScreen(
    onOpenDetail: (MediaItem) -> Unit,
) {
    val vm: MoviesViewModel = koinViewModel()
    val homeState by vm.home.collectAsState()
    val searchResults by vm.search.collectAsState()

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = homeState) {
            is MoviesViewModel.HomeState.Loading -> LoadingState()
            is MoviesViewModel.HomeState.Failed -> ErrorState(
                message = state.message,
                onRetry = vm::loadHome,
            )
            is MoviesViewModel.HomeState.Ready -> {
                if (searching && query.isNotBlank()) {
                    SearchResults(
                        results = searchResults,
                        onItemClick = onOpenDetail,
                    )
                } else {
                    FeedContent(
                        feed = state.feed,
                        onItemClick = onOpenDetail,
                    )
                }
            }
        }

        TopBar(
            query = query,
            searching = searching,
            onQueryChange = { q ->
                query = q
                vm.search(q)
            },
            onToggleSearch = {
                searching = !searching
                if (!searching) {
                    query = ""
                    vm.clearSearch()
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun FeedContent(
    feed: com.app.nepallivetv.domain.model.HomeFeed,
    onItemClick: (MediaItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            MovieHero(
                items = feed.hero,
                onPlay = onItemClick,
                onOpenDetail = onItemClick,
            )
        }
        items(feed.rows, key = { it.title }) { row ->
            MovieRow(row = row, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun SearchResults(
    results: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 120.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No results yet",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try a movie or show name. Catalog refresh is best-effort.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 92.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(results, key = { "${it.kind}-${it.id}" }) { item ->
            MoviePoster(
                item = item,
                onClick = { onItemClick(item) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TopBar(
    query: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xCC0A0A0F),
                        1.0f to Color.Transparent,
                    ),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (!searching) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Movies & Shows",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
        } else {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search titles",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x22FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = BrandRed,
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
        IconButton(onClick = onToggleSearch) {
            Icon(
                imageVector = if (searching) Icons.Default.Close else Icons.Default.Search,
                contentDescription = if (searching) "Close search" else "Search",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 80.dp, bottom = 28.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1B0E12),
                                Color(0xFF0A0A0F),
                            ),
                        ),
                    ),
            )
        }
        items(4) { rowIndex ->
            Column(modifier = Modifier.padding(top = 18.dp)) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(width = 140.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x22FFFFFF)),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 165.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "Catalog offline",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "Try again", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

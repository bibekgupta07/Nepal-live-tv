package com.app.nepallivetv.presentation.screens.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nepallivetv.BuildConfig
import com.app.nepallivetv.data.model.Movie
import com.app.nepallivetv.presentation.viewmodel.MoviesViewModel
import org.koin.androidx.compose.koinViewModel

sealed class MoviesNavigationState {
    object Root : MoviesNavigationState()
    data class SeriesDetail(val seriesName: String) : MoviesNavigationState()
}

data class FolderData(val name: String, val thumbUrl: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    onNavigateToPlayer: (String, String) -> Unit // title, url
) {
    val viewModel = koinViewModel<MoviesViewModel>()
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var navState by remember { mutableStateOf<MoviesNavigationState>(MoviesNavigationState.Root) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Films, 1 = Series

    var searchHistory by remember { mutableStateOf("") }

    // Reset navigation if search query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            navState = MoviesNavigationState.Root
            searchHistory = searchQuery
        }
    }

    BackHandler(enabled = navState != MoviesNavigationState.Root) {
        navState = MoviesNavigationState.Root
        // Restore search history so search isn't wiped out when going back
        if (searchHistory.isNotEmpty()) {
            viewModel.onSearchQueryChange(searchHistory)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navState != MoviesNavigationState.Root) {
                    IconButton(onClick = { 
                        navState = MoviesNavigationState.Root 
                        if (searchHistory.isNotEmpty()) {
                            viewModel.onSearchQueryChange(searchHistory)
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                
                Text(
                    text = when(val s = navState) {
                        is MoviesNavigationState.Root -> "Movies & Series"
                        is MoviesNavigationState.SeriesDetail -> s.seriesName
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = if (navState != MoviesNavigationState.Root) 8.dp else 0.dp)
                )
            }

            // Search Bar
            if (navState == MoviesNavigationState.Root) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                // Premium Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(text = "All", isSelected = selectedTab == 0, onClick = { selectedTab = 0 })
                    FilterPill(text = "Movies", isSelected = selectedTab == 1, onClick = { selectedTab = 1 })
                    FilterPill(text = "Series", isSelected = selectedTab == 2, onClick = { selectedTab = 2 })
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (navState == MoviesNavigationState.Root) {
                    // ROOT VIEW: Shows Movies and Series as Premium Cards
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            // GROUP SEARCH RESULTS BY SERIES / MOVIES
                            val targetList = movies.filter { it.name.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true) }
                            
                            val searchFilms = targetList.filter { it.type == "movie" }
                            val searchSeries = targetList.filter { it.type == "series" }
                                .groupBy { it.name }
                                .map { (name, list) -> FolderData(name, list.firstOrNull { !it.thumbnail_url.isNullOrEmpty() }?.thumbnail_url) }

                            if (searchSeries.isNotEmpty()) {
                                items(searchSeries, key = { "search_series_${it.name}" }) { folder ->
                                    val episodeCount = targetList.count { it.name == folder.name && it.type == "series" }
                                    val seasons = targetList.filter { it.name == folder.name && it.type == "series" && it.season != null }
                                                        .map { it.season }.distinct().size
                                    
                                    PremiumSeriesFolderCard(
                                        title = folder.name, 
                                        seasonCount = seasons, 
                                        episodeCount = episodeCount,
                                        thumbUrl = folder.thumbUrl,
                                        onClick = { 
                                            navState = MoviesNavigationState.SeriesDetail(folder.name) 
                                        }
                                    )
                                }
                            }
                            
                            if (searchFilms.isNotEmpty()) {
                                items(searchFilms, key = { "search_film_${it.id}" }) { movie ->
                                    PremiumMovieCard(movie = movie, onClick = { onNavigateToPlayer(movie.title, movie.stream_url) })
                                }
                            }
                        } else {
                            if (selectedTab == 0 || selectedTab == 1) { // All or Movies
                                val films = movies.filter { it.type == "movie" }
                                items(films, key = { "m_${it.id}" }) { movie ->
                                    PremiumMovieCard(movie = movie, onClick = { onNavigateToPlayer(movie.title, movie.stream_url) })
                                }
                            }
                            if (selectedTab == 0 || selectedTab == 2) { // All or Series
                                val seriesData = movies.filter { it.type == "series" }
                                    .groupBy { it.name }
                                    .map { (name, list) -> FolderData(name, list.firstOrNull { !it.thumbnail_url.isNullOrEmpty() }?.thumbnail_url) }
                                
                                items(seriesData, key = { "series_${it.name}" }) { folder ->
                                    val episodeCount = movies.count { it.name == folder.name && it.type == "series" }
                                    val seasons = movies.filter { it.name == folder.name && it.type == "series" && it.season != null }
                                                        .map { it.season }.distinct().size
                                    
                                    PremiumSeriesFolderCard(
                                        title = folder.name, 
                                        seasonCount = seasons, 
                                        episodeCount = episodeCount,
                                        thumbUrl = folder.thumbUrl,
                                        onClick = { navState = MoviesNavigationState.SeriesDetail(folder.name) }
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (movies.isNotEmpty() && navState == MoviesNavigationState.Root && searchQuery.isEmpty()) {
                            item {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMovies()
                                }
                            }
                        }
                    }
                } else if (navState is MoviesNavigationState.SeriesDetail) {
                    val seriesName = (navState as MoviesNavigationState.SeriesDetail).seriesName
                    // If we are searching, grab from targetList, otherwise from all movies
                    val seriesEpisodes = if(searchQuery.isNotEmpty()) {
                         movies.filter { it.name.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true) }
                               .filter { it.name == seriesName && it.type == "series" }
                    } else {
                         movies.filter { it.name == seriesName && it.type == "series" }
                    }

                    val groupedBySeason = seriesEpisodes.groupBy { it.season ?: "Season 1" }.toSortedMap()

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        groupedBySeason.forEach { (seasonName, episodes) ->
                            item {
                                Text(
                                    text = seasonName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            
                            val groupedByQuality = episodes.groupBy { it.quality }.toSortedMap()
                            
                            groupedByQuality.forEach { (quality, qualEpisodes) ->
                                if (quality != "Unknown") {
                                    item {
                                        Text(
                                            text = quality.uppercase(),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                                
                                val sortedEpisodes = qualEpisodes.sortedBy { it.episode }
                                items(sortedEpisodes, key = { "ep_${it.id}" }) { episode ->
                                    PremiumEpisodeCard(episode = episode, onClick = { onNavigateToPlayer(episode.title, episode.stream_url) })
                                }
                            }
                        }
                    }
                }

                if (error != null && movies.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Failed to load movies: $error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                } else if (!isLoading && movies.isEmpty() && error == null) {
                    Text(
                        text = "No movies or series found.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun PremiumMovieCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!movie.thumbnail_url.isNullOrEmpty()) {
                    val fullThumbUrl = if (movie.thumbnail_url.startsWith("/")) {
                        "${BuildConfig.BASE_URL.removeSuffix("/")}${movie.thumbnail_url}"
                    } else movie.thumbnail_url
                    AsyncImage(
                        model = fullThumbUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = movie.quality.uppercase(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Text(text = " • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${movie.size_bytes / (1024 * 1024)} MB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun PremiumSeriesFolderCard(title: String, seasonCount: Int, episodeCount: Int, thumbUrl: String?, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbUrl.isNullOrEmpty()) {
                        val fullThumbUrl = if (thumbUrl.startsWith("/")) "${BuildConfig.BASE_URL.removeSuffix("/")}${thumbUrl}" else thumbUrl
                        AsyncImage(
                            model = fullThumbUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (expanded) title else title.take(40) + if(title.length > 40) "..." else "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$seasonCount Seasons • $episodeCount Episodes total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "View Episodes", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PremiumEpisodeCard(episode: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = episode.episode?.replace("Episode ", "E") ?: "E?",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${episode.size_bytes / (1024 * 1024)} MB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        }
    }
}

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nepallivetv.domain.model.Episode
import com.app.nepallivetv.domain.model.MediaDetail
import com.app.nepallivetv.domain.model.MediaKind
import com.app.nepallivetv.presentation.viewmodel.MoviesViewModel
import com.app.nepallivetv.ui.theme.BrandRed
import org.koin.androidx.compose.koinViewModel

@Composable
fun MovieDetailScreen(
    kind: MediaKind,
    id: String,
    onBack: () -> Unit,
    onPlay: (MediaDetail, Episode?) -> Unit,
) {
    val vm: MoviesViewModel = koinViewModel()
    val state by vm.detail.collectAsState()

    LaunchedEffect(kind, id) {
        vm.loadDetail(kind, id)
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            is MoviesViewModel.DetailState.Loading -> Center { CircularProgressIndicator(color = BrandRed) }
            is MoviesViewModel.DetailState.Failed -> Center {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Couldn't load this title",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { vm.loadDetail(kind, id) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    ) { Text("Retry", color = Color.White) }
                }
            }
            is MoviesViewModel.DetailState.Ready -> DetailContent(
                detail = s.detail,
                onPlay = onPlay,
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x88000000)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: MediaDetail,
    onPlay: (MediaDetail, Episode?) -> Unit,
) {
    // Default to the first episode of the first season for shows.
    val firstEpisode: Episode? = detail.seasons.firstOrNull()?.episodes?.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(
                model = detail.backdrop.ifBlank { detail.poster },
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.55f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            Text(
                text = detail.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (detail.rating != null) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", detail.rating),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (detail.year != null) {
                    Text(
                        text = detail.year.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (!detail.runtime.isNullOrBlank()) {
                    Text(
                        text = detail.runtime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (detail.kind == MediaKind.SHOW) "Series" else "Movie",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BrandRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onPlay(detail, firstEpisode) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = detail.kind == MediaKind.MOVIE || firstEpisode != null,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (detail.kind == MediaKind.SHOW) {
                        if (firstEpisode != null) "Play S${firstEpisode.season}·E${firstEpisode.number}"
                        else "No episodes"
                    } else "Play",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            if (detail.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = detail.overview,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }

            if (detail.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.genres.forEach { genre ->
                        Text(
                            text = genre,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (detail.cast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))
                SectionLabel("Cast")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(detail.cast) { name ->
                        Text(
                            text = name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (detail.kind == MediaKind.SHOW && detail.seasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                EpisodePicker(detail = detail, onPlayEpisode = { ep -> onPlay(detail, ep) })
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun EpisodePicker(
    detail: MediaDetail,
    onPlayEpisode: (Episode) -> Unit,
) {
    var selectedSeasonIdx by remember { mutableIntStateOf(0) }
    val seasons = detail.seasons
    val current = seasons.getOrNull(selectedSeasonIdx) ?: return

    Column {
        SectionLabel("Episodes")
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons.size) { idx ->
                val isSelected = idx == selectedSeasonIdx
                Text(
                    text = "Season ${seasons[idx].number}",
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandRed else Color(0x22FFFFFF))
                        .clickable { selectedSeasonIdx = idx }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Vertical list of episodes — keep at a reasonable max height by using
        // a Column (the whole detail screen is already verticalScroll, so we
        // can't put a LazyColumn here without breaking nested scrolling).
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            current.episodes.forEach { ep ->
                EpisodeRow(ep = ep, onPlay = { onPlayEpisode(ep) })
            }
        }
    }
}

@Composable
private fun EpisodeRow(ep: Episode, onPlay: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x14FFFFFF))
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "S${ep.season} · E${ep.number}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = ep.title.ifBlank { "Episode ${ep.number}" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

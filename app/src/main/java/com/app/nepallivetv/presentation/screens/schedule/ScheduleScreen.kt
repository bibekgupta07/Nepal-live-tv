package com.app.nepallivetv.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.nepallivetv.data.model.Match
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatMatchTime(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString) ?: return ""

        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        isoString
    }
}

fun calculateCountdown(isoString: String, currentTimeMs: Long): String {
    if (isoString.isBlank()) return "Match yet to begin"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString) ?: return "Match yet to begin"
        
        val diffMs = date.time - currentTimeMs
        if (diffMs <= 0) return "Starting soon..."
        
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMs)
        val mins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
        
        if (hours > 0) {
            "Match starts in $hours hrs $mins mins"
        } else {
            "Match starts in $mins mins"
        }
    } catch (e: Exception) {
        "Match yet to begin"
    }
}

@Composable
fun ScheduleScreen(onMatchClick: (String) -> Unit) {
    val viewModel = koinViewModel<SharedViewModel>()
    val matches by viewModel.cricketMatches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    
    // Auto-refresh the UI every minute to keep countdowns accurate
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(60000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Cricket",
                color = SettingTextGray,
                fontSize = 14.sp
            )
            Text(
                text = "Live & Upcoming",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Categories
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formats = matches.map { it.format }.filter { it.isNotBlank() }.distinct()
            val tabs = listOf("All") + formats

            items(tabs) { tab ->
                val isSelected = tab == selectedCategory
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) BrandRed else CardInactiveBg,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { selectedCategory = tab }
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (tab == "All") "All Matches (${matches.size})" else tab,
                            color = if (isSelected) Color.White else SettingTextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (matches.isEmpty() && isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandRed)
            }
        } else if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No live or upcoming matches right now.", color = SettingTextGray)
            }
        } else {
            val filteredMatches = if (selectedCategory == "All") matches else matches.filter { it.format == selectedCategory }
            val sortedMatches = filteredMatches.sortedBy { it.startTime }
            
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedMatches) { match ->
                    MatchCard(match = match, onMatchClick = onMatchClick, currentTimeMs = currentTimeMs)
                }
            }
        }
    }
}

@Composable
fun MatchCard(match: Match, onMatchClick: (String) -> Unit, currentTimeMs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onMatchClick(match.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardInactiveBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (match.state == "LIVE") {
                        Box(modifier = Modifier.size(8.dp).background(BrandRed, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (match.state == "PRE") "UPCOMING" else match.state,
                        color = if (match.state == "LIVE") BrandRed else SettingTextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                
                Text(
                    text = "${match.format} • ${match.subtitle}",
                    color = SettingTextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Teams Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Team 1
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        AsyncImage(
                            model = match.team1?.logo,
                            contentDescription = match.team1?.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = match.team1?.abbr ?: match.team1?.name ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Team 2
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        AsyncImage(
                            model = match.team2?.logo,
                            contentDescription = match.team2?.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = match.team2?.abbr ?: match.team2?.name ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (match.state == "PRE") {
                        Text(
                            text = "Today",
                            color = SettingTextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatMatchTime(match.startTime),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        // Team 1 Score
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 4.dp).height(24.dp)) {
                            if (!match.team1?.overs.isNullOrEmpty()) {
                                Text(
                                    text = "(${match.team1?.overs}) ",
                                    color = SettingTextGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = match.team1?.score ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Team 2 Score
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 4.dp).height(24.dp)) {
                            if (!match.team2?.overs.isNullOrEmpty()) {
                                Text(
                                    text = "(${match.team2?.overs}) ",
                                    color = SettingTextGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = match.team2?.score ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayStatus = if (match.state == "PRE" && match.statusText.contains("MATCH_START", true)) {
                calculateCountdown(match.startTime, currentTimeMs)
            } else {
                match.statusText
            }

            Text(
                text = displayStatus,
                color = SettingTextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
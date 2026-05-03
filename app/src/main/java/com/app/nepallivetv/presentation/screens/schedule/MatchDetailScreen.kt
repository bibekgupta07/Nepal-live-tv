package com.app.nepallivetv.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.presentation.viewmodel.MatchDetailViewModel
import com.app.nepallivetv.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(matchId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<MatchDetailViewModel>()
    val matchDetail by viewModel.matchDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(matchId) {
        viewModel.fetchDetails(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(matchDetail?.title ?: "Match Details", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.fetchDetails(matchId)
                isRefreshing = false
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (isLoading && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandRed)
                }
            } else if (matchDetail == null && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Failed to load match details", color = MaterialTheme.customColors.settingTextGray)
                }
            } else if (matchDetail != null) {
                val detail = matchDetail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = detail.statusText,
                            color = PremiumLightning,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(detail.scorecards) { scorecard ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(scorecard.team, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    Text("${scorecard.score} (${scorecard.overs} ov)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // BATTING HEADER
                                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.customColors.cardInactiveBg).padding(8.dp)) {
                                    Text("BATTING", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                                    Text("R", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("B", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("4s", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("6s", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("SR", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                scorecard.batsmen.forEach { bat ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(3f)) {
                                            Text(bat.name, color = if(bat.isOut) MaterialTheme.colorScheme.onSurface else BrandRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            if (!bat.dismissal?.long.isNullOrBlank()) {
                                                Text(bat.dismissal.long, color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp)
                                            }
                                        }
                                        Text(bat.runs?.toString() ?: "-", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(bat.balls?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(bat.fours?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(bat.sixes?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(bat.strikeRate?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1.5f))
                                    }
                                    HorizontalDivider(color = MaterialTheme.customColors.cardInactiveBg, thickness = 1.dp)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // BOWLING HEADER
                                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.customColors.cardInactiveBg).padding(8.dp)) {
                                    Text("BOWLING", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                                    Text("O", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("M", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("R", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("W", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("ECON", color = MaterialTheme.customColors.settingTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                scorecard.bowlers.forEach { bowl ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(bowl.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                                        Text(bowl.overs?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(bowl.maidens?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(bowl.runs?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(bowl.wickets?.toString() ?: "-", color = BrandRed, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                                        Text(bowl.economy?.toString() ?: "-", color = MaterialTheme.customColors.settingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1.5f))
                                    }
                                    HorizontalDivider(color = MaterialTheme.customColors.cardInactiveBg, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
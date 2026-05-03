package com.app.nepallivetv.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.presentation.viewmodel.MatchDetailViewModel
import com.app.nepallivetv.ui.theme.DarkBg
import com.app.nepallivetv.ui.theme.DarkBgSurface
import com.app.nepallivetv.ui.theme.SettingTextGray
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(matchId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<MatchDetailViewModel>()
    val matchDetail by viewModel.matchDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.fetchDetails(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(matchDetail?.title ?: "Match Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBgSurface)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (matchDetail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Failed to load match details", color = SettingTextGray)
            }
        } else {
            val detail = matchDetail!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = detail.statusText,
                        color = Color(0xFFFFB703),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(detail.scorecards) { scorecard ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(scorecard.team, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("${scorecard.score} (${scorecard.overs} ov)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("BATTING", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(3f))
                                Text("R", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("B", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("4s", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("6s", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("SR", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1.5f))
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            scorecard.batsmen.forEach { bat ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(3f)) {
                                        Text(bat.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        if (!bat.dismissal?.long.isNullOrBlank()) {
                                            Text(bat.dismissal.long, color = SettingTextGray, fontSize = 10.sp)
                                        }
                                    }
                                    Text(bat.runs?.toString() ?: "-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(bat.balls?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(bat.fours?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(bat.sixes?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(bat.strikeRate?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1.5f))
                                }
                                HorizontalDivider(color = Color(0xFF2C2C35), thickness = 0.5.dp)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("BOWLING", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(3f))
                                Text("O", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("M", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("R", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("W", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("ECON", color = SettingTextGray, fontSize = 10.sp, modifier = Modifier.weight(1.5f))
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            scorecard.bowlers.forEach { bowl ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(bowl.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(3f))
                                    Text(bowl.overs?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(bowl.maidens?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(bowl.runs?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(bowl.wickets?.toString() ?: "-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(bowl.economy?.toString() ?: "-", color = SettingTextGray, fontSize = 14.sp, modifier = Modifier.weight(1.5f))
                                }
                                HorizontalDivider(color = Color(0xFF2C2C35), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
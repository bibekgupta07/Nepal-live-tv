package com.app.nepallivetv.presentation.screens.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nepallivetv.presentation.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(themeViewModel: ThemeViewModel = koinViewModel()) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    
    // Mock States for Cast Settings
    var isCastEnabled by remember { mutableStateOf(true) }
    var forceHdCast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- APPEARANCE SECTION ---
            Text(
                text = "Appearance",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            SettingToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Toggle dark theme across the app",
                isChecked = isDarkMode,
                onCheckedChange = { themeViewModel.toggleTheme(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- CASTING & DISPLAY SECTION ---
            Text(
                text = "Casting & Display",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingToggleItem(
                icon = if (isCastEnabled) Icons.Default.CastConnected else Icons.Default.Cast,
                title = "Enable Chromecast",
                subtitle = "Show cast button in video player",
                isChecked = isCastEnabled,
                onCheckedChange = { isCastEnabled = it }
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingToggleItem(
                icon = Icons.Default.HighQuality,
                title = "Force HD on Cast",
                subtitle = "Always push 1080p stream to TV (uses more data)",
                isChecked = forceHdCast,
                onCheckedChange = { forceHdCast = it },
                enabled = isCastEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How Casting Works 📺", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When you cast, your phone sends the video link directly to your TV. Your TV uses its own internet connection to play the video. You can close the app or use your phone as a remote without stopping the video on the TV!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                    else Color.Gray.copy(alpha = 0.1f), 
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                color = if (enabled) MaterialTheme.colorScheme.onBackground else Color.Gray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle, 
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

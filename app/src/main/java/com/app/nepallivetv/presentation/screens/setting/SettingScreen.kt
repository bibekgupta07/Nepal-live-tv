package com.app.nepallivetv.presentation.screens.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tv
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
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingScreen() {
    val viewModel = koinViewModel<SharedViewModel>()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isCastEnabled by viewModel.isCastEnabled.collectAsState()
    
    var forceHdCast by remember { mutableStateOf(false) }
    var liveNotifications by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Account",
            color = SettingTextGray,
            fontSize = 14.sp
        )
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(AccentOrange, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rajesh Sharma",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "rajesh@gmail.com",
                        color = SettingTextGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .background(PremiumBoxBg, RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, PremiumBoxBorder), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Premium",
                            tint = PremiumLightning,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Premium Plan",
                            color = PremiumTextRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit Profile",
                    tint = SettingTextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "APPEARANCE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
        ) {
            Column {
                SettingToggleItem(
                    icon = Icons.Default.DarkMode,
                    iconBgColor = IconBoxMoonBg,
                    iconTintColor = PremiumLightning,
                    title = "Dark Mode",
                    subtitle = "Toggle dark theme across the app",
                    isChecked = isDarkMode,
                    onCheckedChange = { viewModel.toggleTheme(it) }
                )
                SettingClickableItem(
                    icon = Icons.Default.Abc,
                    iconBgColor = IconBoxLanguageBg,
                    iconTintColor = Color(0xFFADD8E6),
                    title = "Language",
                    subtitle = "Nepali"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "CASTING & DISPLAY")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
        ) {
            Column {
                SettingToggleItem(
                    icon = Icons.Default.Tv,
                    iconBgColor = IconBoxCastBg,
                    iconTintColor = Color(0xFF87CEFA),
                    title = "Enable Chromecast",
                    subtitle = "Show cast button in video player",
                    isChecked = isCastEnabled,
                    onCheckedChange = { viewModel.setCastEnabled(it) }
                )
                SettingToggleItem(
                    icon = Icons.Default.Hd,
                    iconBgColor = IconBoxForceHdBg,
                    iconTintColor = Color(0xFFE6E6FA),
                    title = "Force HD on Cast",
                    subtitle = "Always push 1080p to TV",
                    isChecked = forceHdCast,
                    onCheckedChange = { forceHdCast = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "PLAYBACK")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgSurface)
        ) {
            Column {
                SettingClickableItem(
                    icon = Icons.Default.SignalCellularAlt,
                    iconBgColor = IconBoxQualityBg,
                    iconTintColor = PremiumLightning,
                    title = "Stream Quality",
                    subtitle = "Auto (recommended)"
                )
                SettingToggleItem(
                    icon = Icons.Default.Notifications,
                    iconBgColor = IconBoxNotifBg,
                    iconTintColor = PremiumLightning,
                    title = "Live Notifications",
                    subtitle = "Alerts for your favorite channels",
                    isChecked = liveNotifications,
                    onCheckedChange = { liveNotifications = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = SettingSectionText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = iconTintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle, 
                color = SettingTextGray,
                fontSize = 12.sp
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandRed,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF2C2C35),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {  }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = iconTintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle, 
                color = SettingTextGray,
                fontSize = 12.sp
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Forward",
            tint = SettingTextGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

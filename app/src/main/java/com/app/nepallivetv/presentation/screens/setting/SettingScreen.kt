package com.app.nepallivetv.presentation.screens.setting

import androidx.compose.foundation.background
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
    val authViewModel = koinViewModel<com.app.nepallivetv.presentation.viewmodel.AuthViewModel>()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isCastEnabled by viewModel.isCastEnabled.collectAsState()
    
    val userName by viewModel.datastorePreferences.userNameFlow.collectAsState(initial = "User")
    val userEmail by viewModel.datastorePreferences.userEmailFlow.collectAsState(initial = "user@gmail.com")
    val userPhone by viewModel.datastorePreferences.userPhoneFlow.collectAsState(initial = "No Phone Saved")

    var forceHdCast by remember { mutableStateOf(false) }
    var liveNotifications by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Account",
            color = MaterialTheme.customColors.settingTextGray,
            fontSize = 14.sp
        )
        Text(
            text = "Settings",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        text = userName?.take(1)?.uppercase() ?: "U",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName ?: "User",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userEmail ?: "user@gmail.com",
                        color = MaterialTheme.customColors.settingTextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = userPhone ?: "",
                        color = MaterialTheme.customColors.settingTextGray,
                        fontSize = 14.sp
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.customColors.settingTextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "APPEARANCE")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(title = "ACCOUNT")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { authViewModel.logout() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Logout", color = Color(0xFFFF4C4C), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.customColors.settingTextGray,
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.customColors.settingTextGray,
                fontSize = 13.sp
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandRed,
                uncheckedThumbColor = MaterialTheme.customColors.settingTextGray,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
            .clickable { }
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.customColors.settingTextGray,
                fontSize = 13.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.customColors.settingTextGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
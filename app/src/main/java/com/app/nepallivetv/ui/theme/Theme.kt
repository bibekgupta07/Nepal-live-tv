package com.app.nepallivetv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandRed,
    secondary = AccentOrange,
    tertiary = BrandRed,
    background = DarkBg,
    surface = DarkBgSurface,
    surfaceVariant = CardDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = SettingTextGrayDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    secondary = AccentOrange,
    tertiary = BrandRed,
    background = LightBg,
    surface = LightBgSurface,
    surfaceVariant = CardLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF13131A),
    onSurface = Color(0xFF13131A),
    onSurfaceVariant = SettingTextGrayLight
)

@Composable
fun NepalLiveTvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// CompositionLocals or extensions to access custom colors (e.g. CardInactiveBg) dynamically
val MaterialTheme.customColors: CustomColors
    @Composable
    get() = if (isSystemInDarkTheme()) DarkCustomColors else LightCustomColors

data class CustomColors(
    val cardInactiveBg: Color,
    val bottomNavBg: Color,
    val settingTextGray: Color
)

val DarkCustomColors = CustomColors(
    cardInactiveBg = CardInactiveBgDark,
    bottomNavBg = BottomNavBgDark,
    settingTextGray = SettingTextGrayDark
)

val LightCustomColors = CustomColors(
    cardInactiveBg = CardInactiveBgLight,
    bottomNavBg = BottomNavBgLight,
    settingTextGray = SettingTextGrayLight
)
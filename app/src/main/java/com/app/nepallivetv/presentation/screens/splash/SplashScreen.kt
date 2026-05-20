package com.app.nepallivetv.presentation.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.app.nepallivetv.R
import com.app.nepallivetv.presentation.navigation.PreLoginGraph
import com.app.nepallivetv.presentation.navigation.PostLoginGraph
import com.app.nepallivetv.presentation.navigation.LoginRoute
import com.app.nepallivetv.presentation.navigation.SplashRoute
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, authToken: String?) {
    val scale = remember { Animatable(0.5f) }
    
    // Animate logo once
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Wait for auth token to be loaded and animation to have likely finished
    LaunchedEffect(authToken) {
        if (authToken != "LOADING") {
            // Ensure minimum splash screen duration
            delay(1200)
            if (authToken.isNullOrEmpty()) {
                navController.navigate(LoginRoute) {
                    popUpTo(SplashRoute) { inclusive = true }
                }
            } else {
                navController.navigate(PostLoginGraph) {
                    popUpTo(PreLoginGraph) { inclusive = true }
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Drishya",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Live TV, on every screen",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
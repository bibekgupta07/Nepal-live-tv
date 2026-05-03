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
                navController.navigate(PreLoginGraph) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {
                navController.navigate(PostLoginGraph) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0005)), // Dark plum color matching background
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
                text = "Nepal Live TV",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stream Anything, Anytime",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
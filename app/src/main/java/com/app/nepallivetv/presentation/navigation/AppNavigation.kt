package com.app.nepallivetv.presentation.navigation

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.nepallivetv.LocalFullScreenMode
import com.app.nepallivetv.LocalPipMode
import com.app.nepallivetv.presentation.screens.home.HomeScreen
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.presentation.screens.mylist.MyListScreen
import com.app.nepallivetv.presentation.screens.setting.SettingScreen
import com.app.nepallivetv.presentation.screens.schedule.ScheduleScreen
import com.app.nepallivetv.presentation.screens.tvlist.TvListScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

import com.app.nepallivetv.ui.theme.customColors
import com.app.nepallivetv.presentation.screens.schedule.MatchDetailScreen

import com.app.nepallivetv.presentation.screens.auth.LoginScreen
import com.app.nepallivetv.presentation.screens.auth.RegisterScreen
import com.app.nepallivetv.presentation.viewmodel.AuthViewModel
import com.app.nepallivetv.updater.UpdateViewModel
import com.app.nepallivetv.updater.UpdateManager
import com.app.nepallivetv.updater.UpdateDialog

@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object HomeRoute
@Serializable object TvListRoute
@Serializable object ScheduleRoute
@Serializable data class MatchDetailRoute(val matchId: String)
@Serializable object MyListRoute
@Serializable object SettingRoute

@Composable
fun AppNavigation() {
    val isInPipMode = LocalPipMode.current
    val isFullScreen = LocalFullScreenMode.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val authViewModel = koinViewModel<AuthViewModel>()
    val authToken by authViewModel.isLoggedIn.collectAsState(initial = null)

    val updateViewModel = koinViewModel<UpdateViewModel>()
    val updateState by updateViewModel.updateState.collectAsState()

    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Auto-logout effect
    LaunchedEffect(authToken) {
        if (authToken == null && currentDestination?.route?.contains("Login") != true && currentDestination?.route?.contains("Register") != true) {
            navController.navigate(LoginRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(isBottomBarVisible, currentDestination) {
        if (isBottomBarVisible && !isInPipMode) {
            delay(5000)
            isBottomBarVisible = false
        }
    }

    val showBottomBar = !isFullScreen && !isInPipMode && !isLandscape && 
            currentDestination?.route?.contains("Login") != true && 
            currentDestination?.route?.contains("Register") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    AppBottomNavigation(
                        currentDestinationRoute = currentDestination?.route,
                        onNavigateTo = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                isBottomBarVisible = true
            }
        }
    ) { innerPadding ->
        if (updateState is UpdateManager.UpdateResult.UpdateAvailable) {
            UpdateDialog(
                updateResult = updateState as UpdateManager.UpdateResult.UpdateAvailable,
                onDismiss = { updateViewModel.dismissUpdate() },
                onConfirm = { url -> updateViewModel.downloadAndInstall(url) }
            )
        }

        NavHost(
            navController = navController,
            startDestination = if (authToken.isNullOrEmpty()) LoginRoute else HomeRoute,
            modifier = Modifier.fillMaxSize().then(if (isFullScreen || isLandscape) Modifier else Modifier.padding(innerPadding))
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute)
                    }
                )
            }
            
            composable<RegisterRoute> {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(LoginRoute) {
                            popUpTo(RegisterRoute) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable<HomeRoute> {
                HomeScreen()
            }
            
            composable<TvListRoute> {
                TvListScreen()
            }
            
            composable<ScheduleRoute> {
                ScheduleScreen(onMatchClick = { matchId ->
                    navController.navigate(MatchDetailRoute(matchId))
                })
            }
            
            composable<MatchDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MatchDetailRoute>()
                MatchDetailScreen(matchId = route.matchId, onBack = { navController.popBackStack() })
            }
            
            composable<MyListRoute> {
                MyListScreen()
            }
            
            composable<SettingRoute> {
                SettingScreen()
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentDestinationRoute: String?, onNavigateTo: (Any) -> Unit) {
    val tabs = listOf(
        Triple("Home", Icons.Default.Home, HomeRoute),
        Triple("TV List", Icons.Default.LiveTv, TvListRoute),
        Triple("Schedule", Icons.Default.CalendarMonth, ScheduleRoute),
        Triple("My List", Icons.Default.Bookmarks, MyListRoute),
        Triple("Settings", Icons.Default.Settings, SettingRoute)
    )

    NavigationBar(
        containerColor = MaterialTheme.customColors.bottomNavBg,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp // Flat design as requested
    ) {
        tabs.forEach { (label, icon, routeObject) ->
            val isSelected = currentDestinationRoute?.contains(routeObject::class.simpleName ?: "") == true
            
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateTo(routeObject) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = label, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                },
                label = { Text(label, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

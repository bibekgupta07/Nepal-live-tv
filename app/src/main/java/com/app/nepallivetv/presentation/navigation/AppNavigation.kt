package com.app.nepallivetv.presentation.navigation

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.nepallivetv.presentation.screens.home.HomeScreen
import com.app.nepallivetv.presentation.SharedViewModel
import com.app.nepallivetv.presentation.screens.mylist.MyListScreen
import com.app.nepallivetv.presentation.screens.setting.SettingScreen
import com.app.nepallivetv.presentation.screens.schedule.ScheduleScreen
import com.app.nepallivetv.presentation.screens.tvlist.TvListScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable object HomeRoute
@Serializable object TvListRoute
@Serializable object ScheduleRoute
@Serializable object MyListRoute
@Serializable object SettingRoute

@Composable
fun AppNavigation(isInPipMode: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Share one instance of SharedViewModel across the tabs that need it
    val sharedViewModel = koinViewModel<SharedViewModel>()

    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Auto-hide bottom bar after 5 seconds
    LaunchedEffect(isBottomBarVisible, currentDestination) {
        if (isBottomBarVisible && !isInPipMode) {
            delay(5000)
            isBottomBarVisible = false
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible && !isInPipMode,
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
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.pointerInput(Unit) {
            // Wake up the bottom bar whenever the user taps the screen
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                isBottomBarVisible = true
            }
        }
    ) { innerPadding ->
        // We do NOT apply innerPadding to the NavHost if we want the VideoPlayer to be truly edge-to-edge.
        // Instead, individual screens handle their own bottom padding if needed.
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = sharedViewModel,
                    isInPipMode = isInPipMode,
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
            
            composable<TvListRoute> {
                TvListScreen(
                    viewModel = sharedViewModel,
                    isInPipMode = isInPipMode,
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
            
            composable<ScheduleRoute> {
                ScheduleScreen()
            }
            
            composable<MyListRoute> {
                MyListScreen(
                    viewModel = sharedViewModel,
                    isInPipMode = isInPipMode,
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
            
            composable<SettingRoute> {
                SettingScreen()
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentDestinationRoute: String?, onNavigateTo: (Any) -> Unit) {
    // We inspect the fully qualified class names that Navigation Serialization uses behind the scenes.
    // Or we map strictly to our objects.
    val tabs = listOf(
        Triple("Home", Icons.Default.Home, HomeRoute),
        Triple("TV List", Icons.Default.LiveTv, TvListRoute),
        Triple("Schedule", Icons.Default.CalendarMonth, ScheduleRoute),
        Triple("My List", Icons.Default.Bookmarks, MyListRoute),
        Triple("Settings", Icons.Default.Settings, SettingRoute)
    )

    NavigationBar(
        containerColor = Color(0xFF13131A),
        contentColor = Color.Gray,
        tonalElevation = 8.dp
    ) {
        tabs.forEach { (label, icon, routeObject) ->
            // Check if current destination matches the class name of the route object
            val isSelected = currentDestinationRoute?.contains(routeObject::class.simpleName ?: "") == true
            
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateTo(routeObject) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = label, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                },
                label = { Text(label, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent, // Removed standard pill indicator
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray
                )
            )
        }
    }
}
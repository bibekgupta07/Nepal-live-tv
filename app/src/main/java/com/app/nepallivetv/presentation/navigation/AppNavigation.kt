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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
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
import androidx.navigation.compose.navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.nepallivetv.LocalFullScreenMode
import com.app.nepallivetv.LocalPipMode
import com.app.nepallivetv.presentation.screens.home.HomeScreen
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.presentation.screens.movies.MovieDetailScreen
import com.app.nepallivetv.presentation.screens.movies.MoviePlayerScreen
import com.app.nepallivetv.presentation.screens.movies.MoviesScreen
import com.app.nepallivetv.presentation.screens.setting.SettingScreen
import com.app.nepallivetv.domain.model.MediaKind
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

import com.app.nepallivetv.presentation.screens.splash.SplashScreen
import com.app.nepallivetv.updater.UpdateViewModel
import com.app.nepallivetv.updater.UpdateManager
import com.app.nepallivetv.updater.UpdateDialog

@Serializable object HomeRoute
@Serializable object MoviesRoute
@Serializable object SettingRoute
@Serializable object RootGraph

@Serializable
data class MovieDetailRoute(val kind: String, val slug: String)

// Native ExoPlayer player route. `idEpisode` is null for movies.
@Serializable
data class MoviePlayerRoute(
    val kind: String,
    val slug: String,
    val title: String,
    val idEpisode: Long? = null,
)

@Serializable object SplashRoute

@Composable
fun AppNavigation() {
    val isInPipMode = LocalPipMode.current
    val isFullScreen = LocalFullScreenMode.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val updateViewModel = koinViewModel<UpdateViewModel>()
    val updateState by updateViewModel.updateState.collectAsState()

    var isBottomBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isBottomBarVisible, currentDestination) {
        if (isBottomBarVisible && !isInPipMode) {
            delay(5000)
            isBottomBarVisible = false
        }
    }

    val showBottomBar = !isFullScreen && !isInPipMode && !isLandscape &&
        currentDestination?.route?.contains("Splash") != true

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
            startDestination = RootGraph,
            modifier = Modifier.fillMaxSize().then(if (isFullScreen || isLandscape) Modifier else Modifier.padding(innerPadding))
        ) {
            navigation<RootGraph>(startDestination = SplashRoute) {
                composable<SplashRoute> {
                    SplashScreen(navController = navController)
                }

                composable<HomeRoute> {
                    HomeScreen()
                }

                composable<MoviesRoute> {
                    MoviesScreen(
                        onOpenDetail = { item ->
                            navController.navigate(
                                MovieDetailRoute(
                                    kind = if (item.kind == MediaKind.SHOW) "show" else "movie",
                                    slug = item.id,
                                ),
                            )
                        },
                    )
                }

                composable<MovieDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<MovieDetailRoute>()
                    val kind = if (route.kind == "show") MediaKind.SHOW else MediaKind.MOVIE
                    MovieDetailScreen(
                        kind = kind,
                        id = route.slug,
                        onBack = { navController.popBackStack() },
                        onPlay = { detail, episode ->
                            val playerRoute = if (detail.kind == MediaKind.SHOW && episode != null) {
                                MoviePlayerRoute(
                                    kind = "show",
                                    slug = detail.id,
                                    title = "${detail.title}  ·  S${episode.season}·E${episode.number}",
                                    idEpisode = episode.idEpisode,
                                )
                            } else {
                                MoviePlayerRoute(
                                    kind = "movie",
                                    slug = detail.id,
                                    title = detail.title,
                                    idEpisode = null,
                                )
                            }
                            navController.navigate(playerRoute)
                        },
                    )
                }

                composable<MoviePlayerRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<MoviePlayerRoute>()
                    MoviePlayerScreen(
                        title = route.title,
                        kind = route.kind,
                        slug = route.slug,
                        idEpisode = route.idEpisode,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<SettingRoute> {
                    SettingScreen()
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentDestinationRoute: String?, onNavigateTo: (Any) -> Unit) {
    val tabs = listOf(
        Triple("Home", Icons.Default.Home, HomeRoute),
        Triple("Movies", Icons.Default.Movie, MoviesRoute),
        Triple("Settings", Icons.Default.Settings, SettingRoute)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
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

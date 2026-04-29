package com.app.nepallivetv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.nepallivetv.presentation.MainViewModel
import com.app.nepallivetv.presentation.ui.MainScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

/**
 * Type-safe definition for the Main Screen route.
 */
@Serializable
object MainRoute

/**
 * Coordinator pattern using Compose Navigation.
 * This encapsulates the navigation graph and dependency injection, 
 * keeping the UI components "dumb" and decoupled from ViewModels where appropriate.
 */
@Composable
fun AppNavigation(
    isInPipMode: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainRoute
    ) {
        composable<MainRoute> {
            // Retrieve the ViewModel inside the route, preventing it from leaking into the Activity
            val mainViewModel = koinViewModel<MainViewModel>()
            
            MainScreen(
                viewModel = mainViewModel,
                isInPipMode = isInPipMode
            )
        }
    }
}

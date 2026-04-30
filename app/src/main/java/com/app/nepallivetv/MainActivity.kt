package com.app.nepallivetv

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import com.app.nepallivetv.presentation.ThemeViewModel
import com.app.nepallivetv.presentation.navigation.AppNavigation
import com.app.nepallivetv.ui.theme.NepalLiveTvTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * MainActivity serves as the single entry point to the Compose UI.
 * Handles the Android Native Splash Screen and System-level Picture-in-Picture mode overrides.
 */
class MainActivity : AppCompatActivity() {
    
    private val themeViewModel: ThemeViewModel by viewModel()
    
    // State to pass to Compose to notify if the app is currently small and floating
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Intercepts the app launch and displays our dark custom Splash Screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 2. Extends the app all the way behind the status bar and navigation bar
        enableEdgeToEdge()
        
        // 3. Render the Compose UI Tree using our Navigation Coordinator
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            
            NepalLiveTvTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(isInPipMode = isInPipMode)
                }
            }
        }
    }

    /**
     * Triggered when the user presses the Android "Home" or "Recent Apps" button.
     * Instead of pausing the app, we check if a video is playing. If it is, we ask
     * the Android system to shrink our app into a floating Picture-in-Picture window!
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Hardcoded PiP check removed, handled gracefully by OS if not possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Fired by the system whenever the app actually shrinks into a PiP window or expands back to normal.
     * We capture this state and pass it down to Compose so it can hide non-video UI elements.
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}

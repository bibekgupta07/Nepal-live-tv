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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.presentation.navigation.AppNavigation
import com.app.nepallivetv.ui.theme.NepalLiveTvTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

val LocalPipMode = compositionLocalOf { false }
val LocalFullScreenMode = compositionLocalOf { false }

class MainActivity : AppCompatActivity() {
    
    private val sharedViewModel: SharedViewModel by viewModel()
    
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val isDarkMode by sharedViewModel.isDarkMode.collectAsState()
            val isFullScreen by sharedViewModel.isFullScreen.collectAsState()
            
            NepalLiveTvTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(
                    LocalPipMode provides isInPipMode,
                    LocalFullScreenMode provides isFullScreen
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Only enter PiP if a video is actively playing AND we are in full screen / landscape
                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (sharedViewModel.currentStreamUrl.value != null && isLandscape) {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            // Restored from PiP, ensure we remain in full screen / landscape
            sharedViewModel.setFullScreen(true)
        }
    }
}

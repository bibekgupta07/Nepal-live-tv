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
import com.app.nepallivetv.presentation.viewmodel.ThemeViewModel
import com.app.nepallivetv.presentation.navigation.AppNavigation
import com.app.nepallivetv.ui.theme.NepalLiveTvTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    
    private val themeViewModel: ThemeViewModel by viewModel()
    
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
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
    }
}

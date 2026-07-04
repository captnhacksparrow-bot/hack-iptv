package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.IptvRepository
import com.example.ui.IptvDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.IptvViewModel

class MainActivity : ComponentActivity() {
    private var isInPipModeState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = IptvRepository(this, database.iptvDao())
        
        val viewModel: IptvViewModel by viewModels {
            IptvViewModel.Factory(application, repository)
        }

        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            MyApplicationTheme(theme = appTheme) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    com.example.ui.SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    IptvDashboard(
                        viewModel = viewModel,
                        isInPipMode = isInPipModeState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setAutoEnterEnabled(true)
            }
            enterPictureInPictureMode(params.build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }
}

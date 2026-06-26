package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.IptvRepository
import com.example.ui.IptvDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.IptvViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize local Room database and repository
        val database = AppDatabase.getDatabase(this)
        val repository = IptvRepository(this, database.iptvDao())

        // Create ViewModel
        val viewModel: IptvViewModel by viewModels {
            IptvViewModel.Factory(application, repository)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { androidx.compose.runtime.mutableStateOf(true) }

                if (showSplash) {
                    com.example.ui.SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        IptvDashboard(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

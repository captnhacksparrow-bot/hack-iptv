package com.example.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startExitAnimation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val exitAlpha by animateFloatAsState(
        targetValue = if (startExitAnimation) 0f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "ExitFade"
    )

    val coroutineScope = rememberCoroutineScope()
    fun exitSplash() {
        if (startExitAnimation) return
        coroutineScope.launch {
            startExitAnimation = true
            delay(600)
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(exitAlpha),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash}"))
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        // Scale video to fill or fit depending on the requirement, here fit center is default for VideoView
                        start()
                    }
                    setOnCompletionListener {
                        exitSplash()
                    }
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )

        Button(
            onClick = { exitSplash() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFB03A),
                contentColor = Color(0xFF07090C)
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .systemBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = "SKIP",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}


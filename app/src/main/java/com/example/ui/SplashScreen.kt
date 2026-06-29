package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startExitAnimation by remember { mutableStateOf(false) }

    // Animations setup
    val transitionState = rememberInfiniteTransition(label = "SplashRotation")
    
    // Rotate helm (static offset to prevent CPU lag on lightweight emulators)
    val rotationAngle = 15f

    // Pulse scale (static scale to prevent CPU lag on lightweight emulators)
    val coinScale = 1.0f

    // Sweep position for golden shine effect (static to prevent CPU lag on lightweight emulators)
    val shineSweep = 150f

    // App title scale & fade-in animation
    val appTitleAlpha = remember { Animatable(0f) }
    val appTitleScale = remember { Animatable(0.7f) }

    // Direct mathematical overshoot easing (avoids CubicBezierEasing root-solver bugs near 1.0)
    val OvershootEasing = Easing { t ->
        val tM1 = t - 1f
        val tension = 1.6f
        (tension + 1f) * tM1 * tM1 * tM1 + tension * tM1 * tM1 + 1f
    }

    // Exit state fade animation
    val exitAlpha by animateFloatAsState(
        targetValue = if (startExitAnimation) 0f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "ExitFade"
    )

    LaunchedEffect(Unit) {
        // Run entrance animations in parallel
        launch {
            appTitleAlpha.animateTo(1f, animationSpec = tween(1200, easing = OvershootEasing))
        }
        launch {
            appTitleScale.animateTo(1f, animationSpec = tween(1000, easing = OvershootEasing))
        }
        
        // Wait for splash duration (2000ms total)
        delay(2000)
        startExitAnimation = true
        // Wait for the exit fade animation to finish (600ms) before invoking callback
        delay(600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090C)) // Deep cosmic space black
            .alpha(exitAlpha),
        contentAlignment = Alignment.Center
    ) {
        // Background ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB03A).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High-End Animated Canvas Logo (Helm & Pirate Gold Coin)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(coinScale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = this.center
                    val radius = size.minDimension / 2.5f

                    // 1. Draw Rotating Pirate Helm/Steering Wheel lines
                    rotate(rotationAngle) {
                        val strokeWidth = 5.dp.toPx()
                        val helmColor = Color(0xFF1E222D)
                        val accentColor = Color(0xFFFFB03A)

                        // Draw main outer wheel ring
                        drawCircle(
                            color = helmColor,
                            radius = radius * 1.15f,
                            style = Stroke(width = strokeWidth)
                        )

                        // Draw 8 Spokes
                        for (i in 0 until 8) {
                            val angleRad = Math.toRadians((i * 45).toDouble())
                            val startX = (center.x + Math.cos(angleRad) * (radius * 0.4f)).toFloat()
                            val startY = (center.y + Math.sin(angleRad) * (radius * 0.4f)).toFloat()
                            val endX = (center.x + Math.cos(angleRad) * (radius * 1.4f)).toFloat()
                            val endY = (center.y + Math.sin(angleRad) * (radius * 1.4f)).toFloat()

                            // Spoke handles extending outward
                            drawLine(
                                color = helmColor,
                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )

                            // Handle knobs
                            val knobX = (center.x + Math.cos(angleRad) * (radius * 1.48f)).toFloat()
                            val knobY = (center.y + Math.sin(angleRad) * (radius * 1.48f)).toFloat()
                            drawCircle(
                                color = accentColor,
                                radius = 6.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(knobX, knobY)
                            )
                        }
                    }

                    // 2. Draw Pirate Gold Coin
                    val goldGradients = listOf(
                        Color(0xFFD49015),
                        Color(0xFFFFD466),
                        Color(0xFFFFB03A),
                        Color(0xFF9A6000)
                    )
                    
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = goldGradients,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        ),
                        radius = radius
                    )

                    // Coin Rim Detail
                    drawCircle(
                        color = Color(0xFF633D00),
                        radius = radius * 0.92f,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 3. Gold Coin Reflection/Shine sweeping across the screen
                    val shineBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(shineSweep, 0f),
                        end = androidx.compose.ui.geometry.Offset(shineSweep + 100f, size.height)
                    )

                    drawCircle(
                        brush = shineBrush,
                        radius = radius
                    )

                    // 4. Stylized Pirate Skull details inside the Coin
                    val skullColor = Color(0xFF2A1900)
                    val eyeRadius = radius * 0.12f
                    val noseSize = radius * 0.10f

                    // Forehead/Main skull dome
                    drawCircle(
                        color = skullColor,
                        radius = radius * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.08f)
                    )

                    // Jaw block
                    drawRect(
                        color = skullColor,
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius * 0.22f, center.y + radius * 0.18f),
                        size = androidx.compose.ui.geometry.Size(radius * 0.44f, radius * 0.24f)
                    )

                    // Eye sockets (Pirate Eye Patch on left eye)
                    // Left Eye Patch (Stretched triangle/oval)
                    drawCircle(
                        color = Color(0xFF0F1115),
                        radius = eyeRadius * 1.1f,
                        center = androidx.compose.ui.geometry.Offset(center.x - radius * 0.18f, center.y - radius * 0.05f)
                    )
                    // Eye patch strap
                    drawLine(
                        color = Color(0xFF0F1115),
                        start = androidx.compose.ui.geometry.Offset(center.x - radius * 0.45f, center.y - radius * 0.25f),
                        end = androidx.compose.ui.geometry.Offset(center.x + radius * 0.45f, center.y + radius * 0.15f),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Right Eye socket (Glinting neon blue skeleton eye!)
                    drawCircle(
                        color = skullColor,
                        radius = eyeRadius,
                        center = androidx.compose.ui.geometry.Offset(center.x + radius * 0.18f, center.y - radius * 0.05f)
                    )
                    drawCircle(
                        color = Color(0xFF5390F5), // Neon Pirate Blue
                        radius = eyeRadius * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(center.x + radius * 0.18f, center.y - radius * 0.05f)
                    )

                    // Skull Nose cavity
                    drawCircle(
                        color = Color(0xFF07090C),
                        radius = noseSize * 0.6f,
                        center = androidx.compose.ui.geometry.Offset(center.x, center.y + radius * 0.12f)
                    )

                    // Teeth vertical notches
                    for (t in -1..1) {
                        val teethX = center.x + t * (radius * 0.10f)
                        drawLine(
                            color = Color(0xFFFFD466),
                            start = androidx.compose.ui.geometry.Offset(teethX, center.y + radius * 0.24f),
                            end = androidx.compose.ui.geometry.Offset(teethX, center.y + radius * 0.38f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Text Title - Capt'n Hack Streams
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(appTitleAlpha.value)
                    .scale(appTitleScale.value)
            ) {
                Text(
                    text = "CAPT'N HACK",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = Color(0xFFFFB03A),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "STREAMS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF5390F5),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 10.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Plundering High-Def Channels",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFF9096A5),
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Linear Progress Loading Bar
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .background(Color(0xFF1E222D)),
                contentAlignment = Alignment.CenterStart
            ) {
                val progressAnimation by transitionState.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ProgressAnimation"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnimation)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF5390F5),
                                    Color(0xFFFFB03A)
                                )
                            )
                        )
                )
            }
        }
    }
}

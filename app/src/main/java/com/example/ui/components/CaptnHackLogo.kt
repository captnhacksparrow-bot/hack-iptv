package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CaptnHackLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    animate: Boolean = true
) {
    // Elegant floating animation for high-end aesthetic
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val floatOffset by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val glowAlpha by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        remember { mutableStateOf(0.6f) }
    }

    Column(
        modifier = modifier
            .offset(y = floatOffset.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon / Logo Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB03A).copy(alpha = 0.25f * glowAlpha),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Stylized vector-like drawing of the pirate skull
            Canvas(modifier = Modifier.size(72.dp)) {
                val width = size.width
                val height = size.height

                // 1. Draw Gold Outline / Halo
                drawCircle(
                    color = Color(0xFFFFB03A),
                    radius = width * 0.48f,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
                )

                // 2. Draw Pirate Hat (Bicorn Shape)
                val hatPath = Path().apply {
                    moveTo(width * 0.15f, height * 0.42f)
                    cubicTo(
                        width * 0.20f, height * 0.25f,
                        width * 0.35f, height * 0.18f,
                        width * 0.50f, height * 0.18f
                    )
                    cubicTo(
                        width * 0.65f, height * 0.18f,
                        width * 0.80f, height * 0.25f,
                        width * 0.85f, height * 0.42f
                    )
                    cubicTo(
                        width * 0.72f, height * 0.38f,
                        width * 0.62f, height * 0.35f,
                        width * 0.50f, height * 0.35f
                    )
                    cubicTo(
                        width * 0.38f, height * 0.35f,
                        width * 0.28f, height * 0.38f,
                        width * 0.15f, height * 0.42f
                    )
                    close()
                }
                drawPath(path = hatPath, color = Color(0xFF1E232B))
                drawPath(path = hatPath, color = Color(0xFFFFB03A), style = Stroke(width = 1.5.dp.toPx()))

                // 3. Draw Blue Bandanna band
                val bandannaPath = Path().apply {
                    moveTo(width * 0.25f, height * 0.40f)
                    cubicTo(
                        width * 0.35f, height * 0.38f,
                        width * 0.65f, height * 0.38f,
                        width * 0.75f, height * 0.40f
                    )
                    lineTo(width * 0.73f, height * 0.46f)
                    cubicTo(
                        width * 0.65f, height * 0.44f,
                        width * 0.35f, height * 0.44f,
                        width * 0.27f, height * 0.46f
                    )
                    close()
                }
                drawPath(path = bandannaPath, color = Color(0xFF3A86FF))

                // Bandanna knot tail details
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 4.dp.toPx(),
                    center = Offset(width * 0.74f, height * 0.45f)
                )

                // 4. Draw Skull Face
                val skullPath = Path().apply {
                    moveTo(width * 0.30f, height * 0.46f)
                    lineTo(width * 0.70f, height * 0.46f)
                    cubicTo(
                        width * 0.72f, height * 0.58f,
                        width * 0.66f, height * 0.65f,
                        width * 0.62f, height * 0.65f
                    )
                    // Jaw
                    lineTo(width * 0.58f, height * 0.74f)
                    lineTo(width * 0.42f, height * 0.74f)
                    lineTo(width * 0.38f, height * 0.65f)
                    cubicTo(
                        width * 0.34f, height * 0.65f,
                        width * 0.28f, height * 0.58f,
                        width * 0.30f, height * 0.46f
                    )
                    close()
                }
                drawPath(path = skullPath, color = Color.White)

                // Eye sockets
                drawCircle(
                    color = Color(0xFF1E232B),
                    radius = 4.5.dp.toPx(),
                    center = Offset(width * 0.42f, height * 0.54f)
                )
                drawCircle(
                    color = Color(0xFF1E232B),
                    radius = 4.5.dp.toPx(),
                    center = Offset(width * 0.58f, height * 0.54f)
                )

                // Nose cavity
                val nosePath = Path().apply {
                    moveTo(width * 0.50f, height * 0.59f)
                    lineTo(width * 0.47f, height * 0.63f)
                    lineTo(width * 0.53f, height * 0.63f)
                    close()
                }
                drawPath(path = nosePath, color = Color(0xFF1E232B))

                // Teeth lines
                drawLine(
                    color = Color(0xFF1E232B),
                    start = Offset(width * 0.47f, height * 0.68f),
                    end = Offset(width * 0.47f, height * 0.73f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF1E232B),
                    start = Offset(width * 0.50f, height * 0.67f),
                    end = Offset(width * 0.50f, height * 0.73f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF1E232B),
                    start = Offset(width * 0.53f, height * 0.68f),
                    end = Offset(width * 0.53f, height * 0.73f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        if (showText) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "CAPT'N HACK",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.dp)
                        .background(Color(0xFFFFB03A))
                )
                Text(
                    text = " STREAMS ",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = Color(0xFFFFB03A),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.dp)
                        .background(Color(0xFFFFB03A))
                )
            }
        }
    }
}

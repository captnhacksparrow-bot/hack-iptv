package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PirateGold,
    secondary = PirateBlue,
    background = CosmicDark,
    surface = CardBackground,
    onPrimary = Color(0xFF3E2200),
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SlateGrey,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val LightColorScheme = DarkColorScheme // Always force dark cinematic theme for TV UX


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to keep our customized brand look consistent
  theme: String = "Gold",
  content: @Composable () -> Unit,
) {
  val basePrimary = when (theme) {
      "Ocean Blue" -> Color(0xFF5390F5)
      "Sail Red" -> Color(0xFFEF5350)
      "Emerald" -> Color(0xFF10B981)
      else -> Color(0xFFFFB03A) // "Gold"
  }

  val colorScheme = darkColorScheme(
      primary = basePrimary,
      secondary = basePrimary,
      background = Color(0xFF0F1115),
      surface = Color(0xFF161920),
      onPrimary = Color(0xFF131722),
      onSecondary = Color.White,
      onBackground = Color(0xFFE2E4EB),
      onSurface = Color(0xFFE2E4EB),
      surfaceVariant = Color(0xFF1E222D),
      onSurfaceVariant = Color(0xFF9096A5),
      outline = Color(0xFF2C313E)
  )

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

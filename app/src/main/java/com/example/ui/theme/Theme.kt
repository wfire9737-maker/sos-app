package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = ProfPrimary,
  onPrimary = ProfOnPrimary,
  primaryContainer = ProfPrimaryContainer,
  onPrimaryContainer = ProfOnPrimaryContainer,
  secondary = ProfSecondary,
  onSecondary = ProfOnSecondary,
  secondaryContainer = ProfSecondaryContainer,
  background = DarkBackground,
  onBackground = TextPrimaryDark,
  surface = DarkSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondaryDark,
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
  primary = ProfPrimary,
  onPrimary = ProfOnPrimary,
  primaryContainer = ProfPrimaryContainer,
  onPrimaryContainer = ProfOnPrimaryContainer,
  secondary = ProfSecondary,
  onSecondary = ProfOnSecondary,
  secondaryContainer = ProfSecondaryContainer,
  background = ProfBackgroundLight,
  onBackground = ProfOnBackgroundLight,
  surface = ProfSurfaceLight,
  onSurface = ProfOnSurfaceLight,
  surfaceVariant = ProfSurfaceVariantLight,
  onSurfaceVariant = ProfOnSurfaceVariantLight,
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF),
  outline = ProfOutlineLight
)

@Composable
fun GuardianTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to maintain the high-visibility safety branding color scheme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  GuardianTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

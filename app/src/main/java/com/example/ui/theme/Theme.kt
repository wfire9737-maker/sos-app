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
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryBlue.copy(alpha = 0.3f),
    onPrimaryContainer = PrimaryContainerBlue,
    secondary = SecondaryCyan,
    onSecondary = OnSecondaryCyan,
    secondaryContainer = SecondaryCyan.copy(alpha = 0.3f),
    onSecondaryContainer = SecondaryContainerCyan,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = SecondaryCyan,
    onSecondary = OnSecondaryCyan,
    secondaryContainer = SecondaryContainerCyan,
    onSecondaryContainer = OnSecondaryContainerCyan,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = HighContrastPrimary,
    onPrimary = HighContrastOnPrimary,
    primaryContainer = HighContrastPrimaryContainerDark,
    onPrimaryContainer = HighContrastOnPrimaryContainerDark,
    secondary = HighContrastSecondary,
    onSecondary = HighContrastOnSecondary,
    secondaryContainer = HighContrastSecondaryContainerDark,
    onSecondaryContainer = HighContrastOnSecondaryContainerDark,
    background = HighContrastBackgroundDark,
    onBackground = HighContrastOnBackgroundDark,
    surface = HighContrastSurfaceDark,
    onSurface = HighContrastOnSurfaceDark,
    surfaceVariant = HighContrastSurfaceVariantDark,
    onSurfaceVariant = HighContrastOnSurfaceVariantDark,
    error = HighContrastErrorDark,
    onError = HighContrastOnErrorDark,
    errorContainer = HighContrastErrorContainerDark,
    onErrorContainer = HighContrastOnErrorContainerDark,
    outline = HighContrastOutlineDark,
    outlineVariant = HighContrastOutlineDark
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = HighContrastPrimary,
    onPrimary = HighContrastOnPrimary,
    primaryContainer = HighContrastPrimaryContainerLight,
    onPrimaryContainer = HighContrastOnPrimaryContainerLight,
    secondary = HighContrastSecondary,
    onSecondary = HighContrastOnSecondary,
    secondaryContainer = HighContrastSecondaryContainerLight,
    onSecondaryContainer = HighContrastOnSecondaryContainerLight,
    background = HighContrastBackgroundLight,
    onBackground = HighContrastOnBackgroundLight,
    surface = HighContrastSurfaceLight,
    onSurface = HighContrastOnSurfaceLight,
    surfaceVariant = HighContrastSurfaceVariantLight,
    onSurfaceVariant = HighContrastOnSurfaceVariantLight,
    error = HighContrastErrorLight,
    onError = HighContrastOnErrorLight,
    errorContainer = HighContrastErrorContainerLight,
    onErrorContainer = HighContrastOnErrorContainerLight,
    outline = HighContrastOutlineLight,
    outlineVariant = HighContrastOutlineLight
)

@Composable
fun GuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> HighContrastDarkColorScheme
        highContrast && !darkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GuardianTheme(darkTheme = darkTheme, highContrast = highContrast, dynamicColor = dynamicColor, content = content)
}

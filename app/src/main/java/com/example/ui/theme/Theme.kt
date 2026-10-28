package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NocPrimaryBlue,
    onPrimary = NocOnPrimary,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = NocSecondaryTeal,
    onSecondary = NocOnPrimary,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0F766E),
    tertiary = NocTertiaryIndigo,
    background = NocLightBackground,
    onBackground = NocTextPrimary,
    surface = NocLightSurface,
    onSurface = NocTextPrimary,
    surfaceVariant = NocLightSurfaceVariant,
    onSurfaceVariant = NocTextSecondary,
    outline = NocLightBorder,
    error = NocCriticalRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = NocDarkPrimaryBlue,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = NocDarkSecondaryTeal,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = NocTertiaryIndigo,
    background = NocDarkBackground,
    onBackground = NocDarkTextPrimary,
    surface = NocDarkSurface,
    onSurface = NocDarkTextPrimary,
    surfaceVariant = NocDarkSurfaceVariant,
    onSurfaceVariant = NocDarkTextSecondary,
    outline = NocDarkBorder,
    error = NocCriticalRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                val barColor = if (darkTheme) NocDarkBackground.toArgb() else NocLightBackground.toArgb()
                it.statusBarColor = barColor
                it.navigationBarColor = barColor
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


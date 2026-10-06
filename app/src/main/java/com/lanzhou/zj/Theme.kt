package com.lanzhou.zj

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A5B92),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF041742),
    secondary = Color(0xFF5A5F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE2F9),
    onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF8A4F63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF38071E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF1B2C62),
    primaryContainer = Color(0xFF33437A),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFFC3C6DD),
    onSecondary = Color(0xFF2C3042),
    secondaryContainer = Color(0xFF424658),
    onSecondaryContainer = Color(0xFFDFE2F9),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF51233A),
    tertiaryContainer = Color(0xFF6D3548),
    onTertiaryContainer = Color(0xFFFFD8E4),
)

@Composable
fun TextArtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

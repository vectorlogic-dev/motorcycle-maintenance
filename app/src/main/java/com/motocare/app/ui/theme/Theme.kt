package com.motocare.app.ui.theme

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
    primary = Color(0xFF176B52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5F2D2),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4C635A),
    tertiary = Color(0xFF3E6374),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D6B7),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513D),
    onPrimaryContainer = Color(0xFFA5F2D2),
    secondary = Color(0xFFB3CCC0),
    tertiary = Color(0xFFA6CDDF),
)

@Composable
fun MotoCareTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

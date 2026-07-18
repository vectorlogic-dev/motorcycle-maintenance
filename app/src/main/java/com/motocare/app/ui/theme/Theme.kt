package com.motocare.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF89F8C6),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4C6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9DA),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFF765A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDF8E),
    onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFF8FBF7),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF8FBF7),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FDBAA),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFF89F8C6),
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1F352B),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9DA),
    tertiary = Color(0xFFF0C34B),
    onTertiary = Color(0xFF3D2F00),
    tertiaryContainer = Color(0xFF594400),
    onTertiaryContainer = Color(0xFFFFDF8E),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF404943),
)

@Immutable
data class MotoCareStatusColors(
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color,
    val info: Color,
    val onInfoContainer: Color,
    val infoContainer: Color,
)

private val LightStatusColors = MotoCareStatusColors(
    success = Color(0xFF006C46),
    onSuccessContainer = Color(0xFF002114),
    successContainer = Color(0xFF8AF8BD),
    warning = Color(0xFF765A00),
    onWarningContainer = Color(0xFF241A00),
    warningContainer = Color(0xFFFFDF8E),
    info = Color(0xFF245F86),
    onInfoContainer = Color(0xFF001E2D),
    infoContainer = Color(0xFFC8E6FF),
)

private val DarkStatusColors = MotoCareStatusColors(
    success = Color(0xFF6FDB9F),
    onSuccessContainer = Color(0xFF002114),
    successContainer = Color(0xFF005233),
    warning = Color(0xFFF0C34B),
    onWarningContainer = Color(0xFF241A00),
    warningContainer = Color(0xFF594400),
    info = Color(0xFF91CDF4),
    onInfoContainer = Color(0xFF001E2D),
    infoContainer = Color(0xFF004C6D),
)

val LocalMotoCareStatusColors = staticCompositionLocalOf { LightStatusColors }

val MaterialTheme.motoCareStatusColors: MotoCareStatusColors
    @Composable get() = LocalMotoCareStatusColors.current

private val MotoCareTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

private val MotoCareShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MotoCareTheme(themeMode: String = "SYSTEM", content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val useDynamicColor = themeMode == "SYSTEM" && Build.VERSION.SDK_INT >= 31
    val colors = if (useDynamicColor) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) DarkColors else LightColors
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMotoCareStatusColors provides if (dark) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = MotoCareTypography,
            shapes = MotoCareShapes,
            content = content,
        )
    }
}

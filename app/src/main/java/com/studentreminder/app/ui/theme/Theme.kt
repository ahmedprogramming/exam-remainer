package com.studentreminder.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.studentreminder.app.data.AppSettings
import com.studentreminder.app.data.ThemeMode
import com.studentreminder.app.util.Formatters

/** App-wide access to user settings + preconfigured formatters. */
val LocalAppSettings = compositionLocalOf { AppSettings() }
val LocalFormatters = compositionLocalOf { Formatters(false) }
val LocalIsDark = compositionLocalOf { false }

private fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()),
        hsv
    )
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun lightScheme(accent: Color): ColorScheme {
    val tertiary = shiftHue(accent, 40f)
    val slate = Color(0xFF64748B)
    return lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = lerp(accent, Color.White, 0.84f),
        onPrimaryContainer = lerp(accent, Color.Black, 0.55f),
        secondary = lerp(accent, slate, 0.55f),
        onSecondary = Color.White,
        secondaryContainer = lerp(accent, Color.White, 0.9f),
        onSecondaryContainer = lerp(accent, Color.Black, 0.6f),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = lerp(tertiary, Color.White, 0.85f),
        onTertiaryContainer = lerp(tertiary, Color.Black, 0.55f),
        background = Color(0xFFF7F7FC),
        onBackground = Color(0xFF1A1B23),
        surface = Color(0xFFF7F7FC),
        onSurface = Color(0xFF1A1B23),
        surfaceVariant = Color(0xFFE9EAF2),
        onSurfaceVariant = Color(0xFF5B5E6E),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFF1F2F8),
        surfaceContainerHigh = Color(0xFFECEDF4),
        surfaceContainerHighest = Color(0xFFE6E7EF),
        outline = Color(0xFFB9BCCB),
        outlineVariant = Color(0xFFDDDFE9),
        error = Color(0xFFDC2626),
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
    )
}

private fun darkScheme(accent: Color): ColorScheme {
    val primary = lerp(accent, Color.White, 0.35f)
    val tertiary = lerp(shiftHue(accent, 40f), Color.White, 0.35f)
    return darkColorScheme(
        primary = primary,
        onPrimary = lerp(accent, Color.Black, 0.7f),
        primaryContainer = lerp(accent, Color.Black, 0.55f),
        onPrimaryContainer = lerp(accent, Color.White, 0.8f),
        secondary = lerp(primary, Color(0xFF94A3B8), 0.5f),
        onSecondary = Color(0xFF111118),
        secondaryContainer = lerp(accent, Color(0xFF1E1E27), 0.75f),
        onSecondaryContainer = lerp(accent, Color.White, 0.8f),
        tertiary = tertiary,
        onTertiary = Color(0xFF111118),
        tertiaryContainer = lerp(tertiary, Color.Black, 0.6f),
        onTertiaryContainer = lerp(tertiary, Color.White, 0.75f),
        background = Color(0xFF111117),
        onBackground = Color(0xFFE6E6EE),
        surface = Color(0xFF111117),
        onSurface = Color(0xFFE6E6EE),
        surfaceVariant = Color(0xFF2A2B36),
        onSurfaceVariant = Color(0xFFB4B6C4),
        surfaceContainerLowest = Color(0xFF0C0C11),
        surfaceContainerLow = Color(0xFF1A1A22),
        surfaceContainer = Color(0xFF1E1E27),
        surfaceContainerHigh = Color(0xFF25252F),
        surfaceContainerHighest = Color(0xFF2D2D38),
        outline = Color(0xFF5A5C6B),
        outlineVariant = Color(0xFF34353F),
        error = Color(0xFFF87171),
        errorContainer = Color(0xFF5B1A1A),
        onErrorContainer = Color(0xFFFECACA),
    )
}

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun StudentReminderTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val accent = Color(settings.accent.argb)
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent)
        else -> lightScheme(accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    val formatters = remember(settings.use24h) { Formatters(settings.use24h) }
    CompositionLocalProvider(
        LocalAppSettings provides settings,
        LocalFormatters provides formatters,
        LocalIsDark provides dark,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

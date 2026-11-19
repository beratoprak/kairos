package app.kairos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Compose tarafında kullanılan tema renkleri ([Palette]'in Color karşılığı). */
@Immutable
data class KairosColors(
    val ground: Color,
    val surface: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val line: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val overdue: Color,
    val scrim: Color,
    val isDark: Boolean,
)

fun Palette.toColors(isDark: Boolean) = KairosColors(
    ground = Color(ground), surface = Color(surface), ink = Color(ink), inkMuted = Color(inkMuted),
    inkFaint = Color(inkFaint), line = Color(line), primary = Color(primary), onPrimary = Color(onPrimary),
    accent = Color(accent), overdue = Color(overdue), scrim = Color(scrim), isDark = isDark,
)

val LocalKairosColors = staticCompositionLocalOf { ThemeFamily.Default.light.toColors(isDark = false) }

/**
 * Material'in TÜM renk yuvaları KAİROS token'larına bağlanır. Böylece bir Material bileşeni
 * (geçiş efekti, seçim rengi, odak halkası vb.) varsayılan mora asla düşemez.
 */
private fun KairosColors.toColorScheme() = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = surface,
    onPrimaryContainer = ink,
    inversePrimary = accent,
    secondary = accent,
    onSecondary = ground,
    secondaryContainer = line,
    onSecondaryContainer = ink,
    tertiary = accent,
    onTertiary = ground,
    tertiaryContainer = line,
    onTertiaryContainer = ink,
    background = ground,
    onBackground = ink,
    surface = surface,
    onSurface = ink,
    surfaceVariant = ground,
    onSurfaceVariant = inkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = ink,
    inverseOnSurface = ground,
    error = overdue,
    onError = ground,
    errorContainer = ground,
    onErrorContainer = overdue,
    outline = inkFaint,
    outlineVariant = line,
    scrim = scrim,
    surfaceBright = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = surface,
    surfaceContainerLow = ground,
    surfaceContainerLowest = ground,
    surfaceDim = ground,
)

/** Köşeler 10–12 dp (KURALLAR §2.9). */
val KairosShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun KairosTheme(
    family: ThemeFamily = ThemeFamily.Default,
    mode: ThemeMode = ThemeMode.Default,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = (if (dark) family.dark else family.light).toColors(dark)
    CompositionLocalProvider(LocalKairosColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = KairosTypography,
            shapes = KairosShapes,
            content = content,
        )
    }
}

/** Ekranlarda kısa erişim: `Kairos.colors.ink`. */
object Kairos {
    val colors: KairosColors
        @Composable get() = LocalKairosColors.current
}

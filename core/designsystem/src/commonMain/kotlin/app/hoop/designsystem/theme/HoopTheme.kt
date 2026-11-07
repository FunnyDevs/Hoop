package app.hoop.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.hoop.designsystem.token.*

private val LocalColors = staticCompositionLocalOf<HoopColors> { error("HoopTheme is missing") }
private val LocalTypography = staticCompositionLocalOf<HoopTypography> { error("HoopTheme is missing") }
private val LocalShapes = staticCompositionLocalOf<HoopShapes> { error("HoopTheme is missing") }
private val LocalSpacing = staticCompositionLocalOf<HoopSpacing> { error("HoopTheme is missing") }
private val LocalSizes = staticCompositionLocalOf<HoopSizes> { error("HoopTheme is missing") }
private val LocalElevation = staticCompositionLocalOf<HoopElevation> { error("HoopTheme is missing") }
private val LocalMotion = staticCompositionLocalOf<HoopMotion> { error("HoopTheme is missing") }

/**
 * App theme: provides the design system tokens and maps Material3 to the same tokens.
 * Every component reads values from [HoopTheme], never its own values.
 */
@Composable
fun HoopTheme(content: @Composable () -> Unit) {
    val fonts = rememberHoopFonts()
    val colors = remember { lightHoopColors() }
    val typography = remember(fonts) { hoopTypography(fonts) }

    val materialScheme = lightColorScheme(
        primary = colors.brand,
        onPrimary = colors.onBrand,
        secondary = colors.accent,
        background = colors.background,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        error = colors.error,
        outline = colors.outlineStrong,
    )

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography,
        LocalShapes provides remember { hoopShapes() },
        LocalSpacing provides remember { hoopSpacing() },
        LocalSizes provides remember { hoopSizes() },
        LocalElevation provides remember { hoopElevation() },
        LocalMotion provides remember { hoopMotion() },
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}

/** Token access: `HoopTheme.colors.brand`, `HoopTheme.typography.bodyMedium`, ... */
object HoopTheme {
    val colors: HoopColors @Composable @ReadOnlyComposable get() = LocalColors.current
    val typography: HoopTypography @Composable @ReadOnlyComposable get() = LocalTypography.current
    val shapes: HoopShapes @Composable @ReadOnlyComposable get() = LocalShapes.current
    val spacing: HoopSpacing @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val sizes: HoopSizes @Composable @ReadOnlyComposable get() = LocalSizes.current
    val elevation: HoopElevation @Composable @ReadOnlyComposable get() = LocalElevation.current
    val motion: HoopMotion @Composable @ReadOnlyComposable get() = LocalMotion.current
}

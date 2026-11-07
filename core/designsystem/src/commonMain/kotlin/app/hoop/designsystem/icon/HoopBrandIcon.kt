package app.hoop.designsystem.icon

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size

enum class HoopBrandIcon { Google, Facebook, Apple }

/** One colored part of a logo. `color == null` means "the theme text color". */
private class IconPart(val pathData: String, val color: Color?)

// Third-party brand logos: the colors are the official brand colors, not the app theme.
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleGreen = Color(0xFF34A853)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleRed = Color(0xFFEA4335)
private val FacebookBlue = Color(0xFF1877F2)
private val FacebookWhite = Color(0xFFFFFFFF)

private const val ICON_VIEWBOX = 24f

private val parts: Map<HoopBrandIcon, List<IconPart>> = mapOf(
    HoopBrandIcon.Google to listOf(
        IconPart("M21.6 12.2c0-.7-.1-1.3-.2-1.9H12v3.6h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.2z", GoogleBlue),
        IconPart("M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.8-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22z", GoogleGreen),
        IconPart("M6.4 13.9a6 6 0 0 1 0-3.8V7.5H3.1a10 10 0 0 0 0 9z", GoogleYellow),
        IconPart("M12 5.9c1.5 0 2.8.5 3.8 1.5l2.8-2.8A10 10 0 0 0 3.1 7.5l3.3 2.6C7.2 7.7 9.4 5.9 12 5.9z", GoogleRed),
    ),
    HoopBrandIcon.Facebook to listOf(
        IconPart("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z", FacebookBlue),
        IconPart("M13.4 20v-6.6h2.2l.4-2.6h-2.6V9.2c0-.8.3-1.3 1.4-1.3H16V5.6a15 15 0 0 0-2-.1c-2.1 0-3.5 1.3-3.5 3.6v1.7H8.2v2.6h2.3V20z", FacebookWhite),
    ),
    HoopBrandIcon.Apple to listOf(
        IconPart("M16.4 12.7c0-2.3 1.9-3.4 2-3.5-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.1-2.8.9-3.5.9s-1.9-.8-3.1-.8c-1.6 0-3 .9-3.8 2.3-1.6 2.8-.4 7 1.2 9.3.8 1.1 1.7 2.4 2.9 2.3 1.2 0 1.6-.7 3-.7s1.8.7 3 .7 2-1.1 2.8-2.2c.9-1.3 1.2-2.5 1.3-2.6-.1 0-2.4-.9-2.4-3.9zM14.2 5.9c.6-.8 1.1-1.8.9-2.9-.9 0-2 .6-2.7 1.4-.6.7-1.1 1.8-1 2.8 1 .1 2.1-.5 2.8-1.3z", null),
    ),
)

@Composable
fun HoopBrandIconGraphic(icon: HoopBrandIcon, size: Dp, defaultColor: Color, modifier: Modifier = Modifier) {
    val paths = remember(icon) {
        parts.getValue(icon).map { PathParser().parsePathString(it.pathData).toPath() to it.color }
    }
    Canvas(modifier.size(size)) {
        val k = this.size.width / ICON_VIEWBOX
        scale(k, k, pivot = Offset.Zero) {
            paths.forEach { (path, color) -> drawPath(path, color ?: defaultColor) }
        }
    }
}

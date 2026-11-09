package app.hoop.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.hoop.designsystem.icon.HoopIcons
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.theme.HoopTheme

private object HoopChipSpec {
    val height = 44.dp
    val map = 30.dp
    const val INACTIVE_ALPHA = 0.95f
    const val NEW_ALPHA = 0.85f
    val newStroke = 2.dp
    val newDash = 4.dp
    const val MAP_RING_FRACTION = 0.34f
    const val MAP_RING_FILL_ALPHA = 0.16f
}

/** Pill selecting a Hoop: a tiny map with the area in the Hoop's [color], and its name. The active one is dark. */
@Composable
fun HoopHoopChip(name: String, color: Color, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Row(
        modifier.height(HoopChipSpec.height)
            .shadow(HoopTheme.elevation.badge / 2, HoopTheme.shapes.pill)
            .clip(HoopTheme.shapes.pill)
            .background(if (active) colors.brand else colors.surface.copy(alpha = HoopChipSpec.INACTIVE_ALPHA))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = HoopTheme.spacing.xs, end = HoopTheme.spacing.md - HoopTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(HoopTheme.spacing.xs + 1.dp),
    ) {
        MiniMap(color, Modifier.size(HoopChipSpec.map).clip(CircleShape))
        Text(name, style = HoopTheme.typography.labelMedium.copy(fontSize = HoopTheme.typography.labelMedium.fontSize * 1.08f, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = if (active) colors.onBrand else colors.onSurface)
    }
}

/** Round dashed button at the end of the Hoop chips: tap it to add a new Hoop. */
@Composable
fun HoopNewHoopChip(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val dash = colors.outlineDashed
    Box(
        modifier.size(HoopChipSpec.height)
            .clip(CircleShape)
            .background(colors.surface.copy(alpha = HoopChipSpec.NEW_ALPHA))
            .drawBehind {
                val stroke = HoopChipSpec.newStroke.toPx()
                drawCircle(
                    dash, radius = (size.minDimension - stroke) / 2f,
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(HoopChipSpec.newDash.toPx(), HoopChipSpec.newDash.toPx()))),
                )
            }
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(HoopIcons.Plus, null, tint = colors.onSurface, modifier = Modifier.size(HoopTheme.sizes.iconSmall))
    }
}

@Composable
private fun MiniMap(color: Color, modifier: Modifier) {
    val colors = HoopTheme.colors
    Canvas(modifier) {
        drawRect(colors.mapLand, size = size)
        drawPath(
            Path().apply { moveTo(size.width * 0.72f, 0f); cubicTo(size.width * 0.8f, size.height * 0.3f, size.width * 0.66f, size.height * 0.6f, size.width * 0.78f, size.height); lineTo(size.width, size.height); lineTo(size.width, 0f); close() },
            colors.mapWater,
        )
        drawOval(colors.mapPark, Offset(-size.width * 0.1f, size.height * 0.15f), Size(size.width * 0.5f, size.height * 0.3f))
        listOf(0.35f, 0.7f).forEach { f ->
            drawLine(colors.mapRoad, Offset(0f, size.height * f), Offset(size.width, size.height * (f - 0.1f)), strokeWidth = size.width * 0.07f, cap = StrokeCap.Round)
            drawLine(colors.mapRoad, Offset(size.width * f, 0f), Offset(size.width * (f + 0.08f), size.height), strokeWidth = size.width * 0.07f, cap = StrokeCap.Round)
        }
        val r = size.width * HoopChipSpec.MAP_RING_FRACTION
        drawCircle(color.copy(alpha = HoopChipSpec.MAP_RING_FILL_ALPHA), r, center)
        drawCircle(color, r, center, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 3f * density))))
    }
}

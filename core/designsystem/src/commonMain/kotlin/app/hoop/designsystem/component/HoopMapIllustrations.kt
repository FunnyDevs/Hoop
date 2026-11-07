package app.hoop.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

/** Geometry of the sign-up map illustrations, in the design reference system (390 wide). */
private object MapIllustrationSpec {
    const val REFERENCE_WIDTH = 390f
    const val ROAD_WIDTH = 10f
    const val RING_STROKE = 2f
    val dash = floatArrayOf(7f, 6f)
    const val AREA_FILL_ALPHA = 0.12f

    // Location step
    val locationHeight = 250.dp
    val locationBadge = 64.dp
    val locationPinGlyph = 30.dp
    const val LOCATION_RING_OUTER = 104f
    const val LOCATION_RING_INNER = 56f
    const val LOCATION_FILL_OUTER_ALPHA = 0.1f

    // Radius step
    const val RADIUS_CENTER_Y = 290f
    const val RADIUS_PX_MIN = 70f
    const val RADIUS_PX_MAX = 280f
    const val DOT_RADIUS = 10f
    const val DOT_BORDER = 3f
    const val PIN_RADIUS = 13f
    val pins = listOf(Offset(112f, 210f), Offset(292f, 236f), Offset(150f, 380f))
}

private fun DrawScope.drawBaseMap(land: Color, water: Color, park: Color, road: Color, width: Float, height: Float, waterPath: Path, parks: List<Pair<Offset, Size>>, roads: List<Pair<Offset, Offset>>) {
    drawRect(land, size = Size(width, height))
    drawPath(waterPath, water)
    parks.forEach { (topLeft, size) -> drawOval(park, topLeft, size) }
    roads.forEach { (from, to) -> drawLine(road, from, to, strokeWidth = MapIllustrationSpec.ROAD_WIDTH, cap = StrokeCap.Round) }
}

/** Map with the user's area highlighted and a pin: location permission step. */
@Composable
fun HoopLocationMap(modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Box(modifier.fillMaxWidth().height(MapIllustrationSpec.locationHeight).clipToBounds(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / MapIllustrationSpec.REFERENCE_WIDTH
            val h = size.height / k
            scale(k, k, pivot = Offset.Zero) {
                val water = Path().apply {
                    moveTo(300f, 0f); cubicTo(320f, 80f, 290f, 150f, 330f, h); lineTo(MapIllustrationSpec.REFERENCE_WIDTH, h)
                    lineTo(MapIllustrationSpec.REFERENCE_WIDTH, 0f); close()
                }
                drawBaseMap(
                    colors.mapLand, colors.mapWater, colors.mapPark, colors.mapRoad, MapIllustrationSpec.REFERENCE_WIDTH, h, water,
                    parks = listOf(Offset(-10f, 20f) to Size(140f, 80f)),
                    roads = listOf(
                        Offset(-20f, 90f) to Offset(410f, 120f), Offset(130f, -10f) to Offset(160f, h + 10f),
                        Offset(-20f, 200f) to Offset(410f, 170f), Offset(270f, -10f) to Offset(240f, h + 10f),
                    ),
                )
                val center = Offset(MapIllustrationSpec.REFERENCE_WIDTH / 2f, h / 2f)
                drawCircle(colors.accent.copy(alpha = MapIllustrationSpec.LOCATION_FILL_OUTER_ALPHA), MapIllustrationSpec.LOCATION_RING_OUTER, center)
                drawCircle(colors.accent, MapIllustrationSpec.LOCATION_RING_OUTER, center, style = Stroke(MapIllustrationSpec.RING_STROKE, pathEffect = PathEffect.dashPathEffect(MapIllustrationSpec.dash)))
                drawCircle(colors.accent.copy(alpha = MapIllustrationSpec.AREA_FILL_ALPHA), MapIllustrationSpec.LOCATION_RING_INNER, center)
                drawCircle(colors.accent, MapIllustrationSpec.LOCATION_RING_INNER, center, style = Stroke(MapIllustrationSpec.RING_STROKE))
            }
        }
        Box(
            Modifier.size(MapIllustrationSpec.locationBadge)
                .shadow(HoopTheme.elevation.card, HoopTheme.shapes.circle)
                .background(colors.surface, HoopTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(HoopIcons.Pin, null, tint = colors.accent, modifier = Modifier.size(MapIllustrationSpec.locationPinGlyph))
        }
    }
}

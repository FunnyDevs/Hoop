package app.hoop.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import app.hoop.designsystem.theme.HoopTheme

/** Geometry of the stylized map, in the design reference system (390 x 844). */
private object MapBackdropSpec {
    const val REFERENCE_WIDTH = 390f
    const val REFERENCE_HEIGHT = 844f
    const val ROAD_WIDTH = 10f
    val parkTopLeft = Offset(0f, 96f) to Size(140f, 88f)
    val parkBottomLeft = Offset(-30f, 580f) to Size(180f, 120f)
    val roads = listOf(
        Offset(-20f, 200f) to Offset(410f, 240f),
        Offset(130f, -10f) to Offset(170f, 860f),
        Offset(-20f, 420f) to Offset(410f, 380f),
        Offset(-20f, 600f) to Offset(410f, 640f),
        Offset(260f, 300f) to Offset(300f, 860f),
    )
}

/**
 * Background that evokes a map. Graphic placeholder until the real map (MapLibre) is integrated.
 */
@Composable
fun HoopMapBackdrop(modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Canvas(modifier.fillMaxSize()) {
        val k = size.width / MapBackdropSpec.REFERENCE_WIDTH
        scale(k, k, pivot = Offset.Zero) {
            drawRect(colors.mapLand, size = Size(MapBackdropSpec.REFERENCE_WIDTH, size.height / k))
            val water = Path().apply {
                moveTo(300f, 0f)
                cubicTo(330f, 120f, 280f, 220f, 330f, 330f)
                cubicTo(380f, 440f, 360f, 480f, 345f, 600f)
                cubicTo(330f, 700f, 330f, 760f, 350f, MapBackdropSpec.REFERENCE_HEIGHT)
                lineTo(MapBackdropSpec.REFERENCE_WIDTH, size.height / k)
                lineTo(MapBackdropSpec.REFERENCE_WIDTH, 0f)
                close()
            }
            drawPath(water, colors.mapWater)
            drawOval(colors.mapPark, MapBackdropSpec.parkTopLeft.first, MapBackdropSpec.parkTopLeft.second)
            drawOval(colors.mapPark, MapBackdropSpec.parkBottomLeft.first, MapBackdropSpec.parkBottomLeft.second)
            MapBackdropSpec.roads.forEach { (from, to) ->
                drawLine(colors.mapRoad, from, to, strokeWidth = MapBackdropSpec.ROAD_WIDTH, cap = StrokeCap.Round)
            }
        }
    }
}

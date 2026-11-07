package app.hoop.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Line icons of the app (24 x 24 viewBox), drawn as vectors and tinted by the caller. */
object HoopIcons {
    private const val VIEWBOX = 24f
    private const val ICON_SIZE = 24
    private const val STROKE = 2f
    private const val THIN_STROKE = 1.8f

    private fun line(name: String, strokeWidth: Float = STROKE, vararg pathData: String): ImageVector =
        ImageVector.Builder(name, ICON_SIZE.dp, ICON_SIZE.dp, VIEWBOX, VIEWBOX).apply {
            pathData.forEach { data ->
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val ArrowBack: ImageVector by lazy { line("ArrowBack", STROKE, "M15 6l-6 6 6 6") }
    val Check: ImageVector by lazy { line("Check", STROKE, "M5 12l5 5 9-10") }
    val Alert: ImageVector by lazy {
        line("Alert", STROKE, "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", "M12 8v5M12 16.5v.01")
    }
    val Eye: ImageVector by lazy {
        line("Eye", THIN_STROKE, "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z")
    }
    val EyeOff: ImageVector by lazy {
        line("EyeOff", THIN_STROKE, "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z", "M4 4l16 16")
    }
    val Mail: ImageVector by lazy {
        line("Mail", THIN_STROKE, "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M3.5 7.5l8.5 6 8.5-6")
    }
    val Pin: ImageVector by lazy {
        ImageVector.Builder("Pin", ICON_SIZE.dp, ICON_SIZE.dp, VIEWBOX, VIEWBOX).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M12 2a7 7 0 0 0-7 7c0 5 7 13 7 13s7-8 7-13a7 7 0 0 0-7-7zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5z",
                ).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }
}

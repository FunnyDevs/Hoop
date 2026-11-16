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
    private const val FLY_TILT = 28f
    private const val FLY_STROKE = 1.2f

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
    val Chat: ImageVector by lazy { line("Chat", THIN_STROKE, "M4 5h16v11H9l-5 4z") }
    val Camera: ImageVector by lazy { line("Camera", THIN_STROKE, "M4 8h3l2-3h6l2 3h3v11H4z", "M12 10a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z") }
    val Pen: ImageVector by lazy { line("Pen", STROKE, "M4 20h4L19 9l-4-4L4 16z") }
    val Bell: ImageVector by lazy { line("Bell", STROKE, "M6 17V11a6 6 0 0 1 12 0v6l1.5 2h-15z", "M10 21h4") }
    val Send: ImageVector by lazy { line("Send", STROKE, "M5 12l14-7-5 14-2-6z") }
    val Plus: ImageVector by lazy { line("Plus", 2.4f, "M12 5v14M5 12h14") }
    val Close: ImageVector by lazy { line("Close", 2.4f, "M6 6l12 12M18 6L6 18") }
    val ArrowUp: ImageVector by lazy { line("ArrowUp", STROKE, "M12 19V6M6 11l6-6 6 6") }
    /** Lighter X and heavier tick of the confirmation of a shot. */
    val CloseLight: ImageVector by lazy { line("CloseLight", STROKE, "M6 6l12 12M18 6L6 18") }
    val CheckHeavy: ImageVector by lazy { line("CheckHeavy", 2.8f, "M5 12l5 5L20 7") }
    val Flash: ImageVector by lazy { line("Flash", STROKE, "M13 3L5 14h6l-1 7 8-11h-6z") }
    val FlashOff: ImageVector by lazy { line("FlashOff", STROKE, "M13 3L5 14h6l-1 7 8-11h-6z", "M4 4l16 16") }
    val FlipCamera: ImageVector by lazy {
        line("FlipCamera", STROKE, "M4 8V5h3M20 8V5h-3M4 16v3h3M20 16v3h-3", "M12 8.8a3.2 3.2 0 1 0 0 6.4 3.2 3.2 0 0 0 0-6.4z")
    }
    val Clock: ImageVector by lazy { line("Clock", STROKE, "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", "M12 7v5l3 2") }
    val Volume: ImageVector by lazy { line("Volume", STROKE, "M4 9v6h4l5 4V5L8 9z", "M16 9a4 4 0 0 1 0 6") }
    val VolumeOff: ImageVector by lazy { line("VolumeOff", STROKE, "M4 9v6h4l5 4V5L8 9z", "M17 9l4 6M21 9l-4 6") }
    val Retake: ImageVector by lazy { line("Retake", STROKE, "M9 7L4 12l5 5", "M4 12h11a5 5 0 0 1 0 10h-3") }

    /** "Ci volo": a swallow banking into a turn (filled, with a thin stroke of the same color), rotated by [FLY_TILT] around the center. */
    val Fly: ImageVector by lazy {
        ImageVector.Builder("Fly", ICON_SIZE.dp, ICON_SIZE.dp, VIEWBOX, VIEWBOX).apply {
            addGroup(rotate = FLY_TILT, pivotX = VIEWBOX / 2f, pivotY = VIEWBOX / 2f)
            addPath(
                pathData = PathParser().parsePathString(
                    "M2.5 8C6 8 9.5 9.5 12 13C14.5 9.5 18 8 21.5 8C19.5 12 16 14.3 13.2 15L14.8 20.5L12 18L9.2 20.5L10.8 15C8 14.3 4.5 12 2.5 8Z",
                ).toNodes(),
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = FLY_STROKE,
                strokeLineJoin = StrokeJoin.Round,
            )
            clearGroup()
        }.build()
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

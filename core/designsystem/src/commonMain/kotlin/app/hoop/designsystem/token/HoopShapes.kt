package app.hoop.designsystem.token

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
class HoopShapes(
    val pill: Shape,
    val card: Shape,
    val chip: Shape,
    val circle: Shape,
    val sheet: Shape,
    val field: Shape,
    val banner: Shape,
    val checkbox: Shape,
)

internal fun hoopShapes() = HoopShapes(
    pill = RoundedCornerShape(percent = 50),
    card = RoundedCornerShape(24.dp),
    chip = RoundedCornerShape(15.dp),
    circle = CircleShape,
    sheet = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    field = RoundedCornerShape(16.dp),
    banner = RoundedCornerShape(14.dp),
    checkbox = RoundedCornerShape(8.dp),
)

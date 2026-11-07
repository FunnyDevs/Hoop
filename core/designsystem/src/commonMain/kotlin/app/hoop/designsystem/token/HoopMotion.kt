package app.hoop.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class HoopMotion(
    val floatDistance: Dp,
    val floatDurationMillis: Int,
    val scrimAlphaOnMedia: Float,
    val playBadgeAlpha: Float,
)

internal fun hoopMotion() = HoopMotion(
    floatDistance = (-7).dp, floatDurationMillis = 2750, scrimAlphaOnMedia = 0.55f, playBadgeAlpha = 0.92f,
)

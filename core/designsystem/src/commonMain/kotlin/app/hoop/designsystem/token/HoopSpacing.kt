package app.hoop.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class HoopSpacing(
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val screenGutter: Dp,
)

internal fun hoopSpacing() = HoopSpacing(
    xxs = 2.dp, xs = 6.dp, sm = 10.dp, md = 16.dp, lg = 24.dp, xl = 32.dp, xxl = 48.dp, screenGutter = 24.dp,
)

package app.hoop.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class HoopElevation(val none: Dp, val badge: Dp, val card: Dp)

internal fun hoopElevation() = HoopElevation(none = 0.dp, badge = 8.dp, card = 14.dp)

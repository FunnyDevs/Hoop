package app.hoop.designsystem.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fixed sizes of controls and icons. */
@Immutable
class HoopSizes(
    val buttonHeight: Dp,
    val iconSmall: Dp,
    val iconMedium: Dp,
    val avatar: Dp,
    val chipHeight: Dp,
    val logoBadgeHeight: Dp,
    val logoMarkSize: Dp,
    val logoMarkStroke: Dp,
    val playBadge: Dp,
    val playGlyph: Dp,
    val borderWidth: Dp,
    val dividerThickness: Dp,
    val fieldHeight: Dp,
    val touchTarget: Dp,
    val checkboxSize: Dp,
    val progressSegmentHeight: Dp,
    val strengthBarHeight: Dp,
    val sliderThumb: Dp,
    val sliderTrack: Dp,
    val bannerIcon: Dp,
    val successBadge: Dp,
    val successGlyph: Dp,
)

internal fun hoopSizes() = HoopSizes(
    buttonHeight = 56.dp, iconSmall = 20.dp, iconMedium = 24.dp,
    avatar = 24.dp, chipHeight = 30.dp, logoBadgeHeight = 40.dp, logoMarkSize = 19.dp, logoMarkStroke = 4.dp,
    playBadge = 44.dp, playGlyph = 16.dp, borderWidth = 1.5.dp, dividerThickness = 1.dp,
    fieldHeight = 56.dp, touchTarget = 44.dp, checkboxSize = 26.dp,
    progressSegmentHeight = 4.dp, strengthBarHeight = 5.dp, sliderThumb = 30.dp, sliderTrack = 6.dp,
    bannerIcon = 20.dp, successBadge = 132.dp, successGlyph = 64.dp,
)

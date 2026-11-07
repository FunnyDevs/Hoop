package app.hoop.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.theme.HoopTheme

private object RingSpec {
    val cornerLarge = 260.dp
    val cornerLargeOffset = (-90).dp
    val cornerSmall = 160.dp
    val cornerSmallOffset = (-40).dp
    const val CORNER_LARGE_ALPHA = 0.28f
    const val CORNER_SMALL_ALPHA = 0.4f
    val ringWidth = 2.dp
    val successRings = listOf(220.dp to 0.5f, 320.dp to 0.3f, 420.dp to 0.18f)
    const val SUCCESS_SHADOW_ALPHA = 0.3f
}

/** Two concentric accent rings peeking from the top-right corner of a screen. */
@Composable
fun BoxScope.HoopCornerRings() {
    val accent = HoopTheme.colors.accent
    Box(
        Modifier.align(Alignment.TopEnd).offset(RingSpec.cornerLargeOffset * -1, RingSpec.cornerLargeOffset)
            .size(RingSpec.cornerLarge).border(RingSpec.ringWidth, accent.copy(alpha = RingSpec.CORNER_LARGE_ALPHA), HoopTheme.shapes.circle),
    )
    Box(
        Modifier.align(Alignment.TopEnd).offset(RingSpec.cornerSmallOffset * -1, RingSpec.cornerSmallOffset)
            .size(RingSpec.cornerSmall).border(RingSpec.ringWidth, accent.copy(alpha = RingSpec.CORNER_SMALL_ALPHA), HoopTheme.shapes.circle),
    )
}

/** Large round badge with an icon surrounded by expanding rings: used by "done" screens. */
@Composable
fun HoopSuccessBadge(icon: ImageVector, contentDescription: String, color: Color, ringColor: Color, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        RingSpec.successRings.forEach { (size, alpha) ->
            Box(Modifier.requiredSize(size).border(RingSpec.ringWidth, ringColor.copy(alpha = alpha), HoopTheme.shapes.circle))
        }
        Box(
            Modifier.size(HoopTheme.sizes.successBadge)
                .shadow(HoopTheme.elevation.card, HoopTheme.shapes.circle, ambientColor = color.copy(alpha = RingSpec.SUCCESS_SHADOW_ALPHA), spotColor = color.copy(alpha = RingSpec.SUCCESS_SHADOW_ALPHA))
                .background(color, HoopTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = HoopTheme.colors.onBrand, modifier = Modifier.size(HoopTheme.sizes.successGlyph))
        }
    }
}

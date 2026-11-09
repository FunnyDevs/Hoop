package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

private object HomeControlsSpec {
    const val FAB_OPEN_DEGREES = 135f
    val badgeDot = 9.dp
    val badgeDotBorder = 2.dp
    const val WORLD_SURFACE_ALPHA = 0.9f
    const val FAB_SHADOW_ALPHA = 0.45f
}

/** Round floating button that rotates into a cross while [open]. */
@Composable
fun HoopFab(contentDescription: String, open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Box(
        modifier.size(HoopTheme.sizes.fab)
            .shadow(HoopTheme.elevation.card, HoopTheme.shapes.circle, ambientColor = colors.accent.copy(alpha = HomeControlsSpec.FAB_SHADOW_ALPHA), spotColor = colors.accent.copy(alpha = HomeControlsSpec.FAB_SHADOW_ALPHA))
            .background(colors.accent, HoopTheme.shapes.circle)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(HoopIcons.Plus, null, tint = colors.onBrand, modifier = Modifier.size(HoopTheme.sizes.iconMedium + 2.dp).rotate(if (open) HomeControlsSpec.FAB_OPEN_DEGREES else 0f))
    }
}

/** Translucent round button over the map, with an optional notification dot. */
@Composable
fun HoopMapIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Boolean = false,
) {
    val colors = HoopTheme.colors
    Box(
        modifier.size(HoopTheme.sizes.touchTarget)
            .shadow(HoopTheme.elevation.badge, HoopTheme.shapes.circle)
            .background(colors.surface.copy(alpha = HomeControlsSpec.WORLD_SURFACE_ALPHA), HoopTheme.shapes.circle)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = colors.onSurface, modifier = Modifier.size(HoopTheme.sizes.iconSmall))
        if (dot) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 11.dp).size(HomeControlsSpec.badgeDot)
                    .background(colors.accent, HoopTheme.shapes.circle).border(HomeControlsSpec.badgeDotBorder, colors.surface, HoopTheme.shapes.circle),
            )
        }
    }
}

/** Floating translucent label over the map, for example "Casa · raggio 2,5 km · 5 post". */
@Composable
fun HoopMapChip(text: String, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Text(
        text,
        modifier.shadow(HoopTheme.elevation.badge / 2, HoopTheme.shapes.chip)
            .background(colors.surface.copy(alpha = 0.92f), HoopTheme.shapes.chip)
            .padding(horizontal = HoopTheme.spacing.sm + HoopTheme.spacing.xxs, vertical = HoopTheme.spacing.xs - 1.dp),
        style = HoopTheme.typography.labelSmall.copy(fontSize = HoopTheme.typography.labelMedium.fontSize), color = colors.onSurface,
    )
}

/** Dark pill used for short confirmations ("Published in Casa"). */
@Composable
fun HoopToast(text: String, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Text(
        text,
        modifier.background(colors.onSurface, HoopTheme.shapes.pill).padding(horizontal = HoopTheme.spacing.md, vertical = HoopTheme.spacing.sm),
        style = HoopTheme.typography.labelMedium, color = colors.onBrand,
    )
}

/** Dots showing the position in a carousel. */
@Composable
fun HoopPageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(HoopTheme.spacing.xs)) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                Modifier.size(width = if (active) 18.dp else 5.dp, height = 5.dp)
                    .background(if (active) colors.onSurface else colors.outlineStrong, HoopTheme.shapes.pill),
            )
        }
    }
}

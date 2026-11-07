package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.hoop.designsystem.theme.HoopTheme

enum class HoopIconButtonStyle { Filled, Elevated, Plain }

/** Round icon button of touch-target size. */
@Composable
fun HoopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HoopIconButtonStyle = HoopIconButtonStyle.Filled,
) {
    val container = when (style) {
        HoopIconButtonStyle.Filled -> HoopTheme.colors.neutralContainer
        HoopIconButtonStyle.Elevated -> HoopTheme.colors.surface
        HoopIconButtonStyle.Plain -> HoopTheme.colors.transparent
    }
    val tint = if (style == HoopIconButtonStyle.Plain) HoopTheme.colors.onSurfaceVariant else HoopTheme.colors.onSurface
    Box(
        modifier = modifier
            .size(HoopTheme.sizes.touchTarget)
            .then(if (style == HoopIconButtonStyle.Elevated) Modifier.shadow(HoopTheme.elevation.badge, HoopTheme.shapes.circle) else Modifier)
            .clip(HoopTheme.shapes.circle)
            .background(container)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(HoopTheme.sizes.iconMedium))
    }
}

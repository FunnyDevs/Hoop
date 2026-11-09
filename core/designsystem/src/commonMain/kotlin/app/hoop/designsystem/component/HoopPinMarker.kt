package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

private object PinSpec {
    val size = 30.dp
    val border = 3.dp
    const val SELECTED_SCALE = 1.45f
    val glyphIcon = 14.dp
    val glyphPlay = 12.dp
}

/** Round pin of a post on the map: yellow "?" for questions, blue camera for photos, pink play for videos. */
@Composable
fun HoopPinMarker(kind: HoopPostKind, selected: Boolean, questionGlyph: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val scale by animateFloatAsState(if (selected) PinSpec.SELECTED_SCALE else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    val color = when (kind) {
        HoopPostKind.Question -> colors.pinYellow
        HoopPostKind.Photo -> colors.pinBlue
        HoopPostKind.Video -> colors.pinPink
    }
    Box(
        modifier.size(PinSpec.size).scale(scale)
            .shadow(if (selected) HoopTheme.elevation.card else HoopTheme.elevation.badge / 2, HoopTheme.shapes.circle)
            .background(color, HoopTheme.shapes.circle)
            .border(PinSpec.border, colors.surface, HoopTheme.shapes.circle)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            HoopPostKind.Question -> Text(questionGlyph, style = HoopTheme.typography.titleMedium.copy(fontSize = HoopTheme.typography.labelLarge.fontSize * 0.95f), color = colors.onSurface)
            HoopPostKind.Photo -> Icon(HoopIcons.Camera, null, tint = colors.onScrim, modifier = Modifier.size(PinSpec.glyphIcon))
            HoopPostKind.Video -> HoopPlayGlyph(colors.onScrim, Modifier.size(PinSpec.glyphPlay))
        }
    }
}

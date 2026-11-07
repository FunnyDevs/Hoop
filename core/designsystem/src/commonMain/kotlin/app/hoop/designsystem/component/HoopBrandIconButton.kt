package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.hoop.designsystem.icon.HoopBrandIcon
import app.hoop.designsystem.icon.HoopBrandIconGraphic
import app.hoop.designsystem.theme.HoopTheme

/** Round button showing the logo of a sign-in provider. */
@Composable
fun HoopBrandIconButton(
    icon: HoopBrandIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(HoopTheme.sizes.buttonHeight)
            .clip(HoopTheme.shapes.circle)
            .background(HoopTheme.colors.surface)
            .border(HoopTheme.sizes.borderWidth, HoopTheme.colors.outlineStrong, HoopTheme.shapes.circle)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HoopBrandIconGraphic(icon, HoopTheme.sizes.iconMedium, HoopTheme.colors.onSurface)
    }
}

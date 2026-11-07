package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.theme.HoopTheme

/** Small tappable suggestion pill, for example a corrected email address. */
@Composable
fun HoopChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(ChipHeight).clip(HoopTheme.shapes.pill).background(HoopTheme.colors.chipContainer)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = HoopTheme.spacing.md - HoopTheme.spacing.xxs * 2),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = HoopTheme.colors.onSurface)
    }
}

private val ChipHeight = 32.dp

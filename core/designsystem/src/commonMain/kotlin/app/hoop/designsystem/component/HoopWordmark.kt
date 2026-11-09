package app.hoop.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hoop.designsystem.theme.HoopTheme

/** The app name with its orange ring, drawn straight over the map with a soft glow for legibility. */
@Composable
fun HoopWordmark(name: String, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm)) {
        Box(Modifier.size(20.dp).border(HoopTheme.sizes.logoMarkStroke, colors.accent, HoopTheme.shapes.circle))
        Text(
            name,
            style = HoopTheme.typography.titleLarge.copy(fontSize = 25.sp, shadow = Shadow(colors.mapLand, Offset.Zero, 10f)),
            color = colors.onSurface,
        )
    }
}

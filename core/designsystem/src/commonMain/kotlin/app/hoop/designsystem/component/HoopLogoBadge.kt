package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import app.hoop.designsystem.theme.HoopTheme

/** The app brand mark: orange ring and name on a white pill. */
@Composable
fun HoopLogoBadge(name: String, modifier: Modifier = Modifier) {
    val spacing = HoopTheme.spacing
    Row(
        modifier = modifier
            .height(HoopTheme.sizes.logoBadgeHeight)
            .shadow(HoopTheme.elevation.badge, HoopTheme.shapes.pill)
            .clip(HoopTheme.shapes.pill)
            .background(HoopTheme.colors.surface)
            .padding(start = spacing.sm, end = spacing.md - spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm - spacing.xxs),
    ) {
        Box(
            Modifier.size(HoopTheme.sizes.logoMarkSize)
                .border(HoopTheme.sizes.logoMarkStroke, HoopTheme.colors.accent, HoopTheme.shapes.circle),
        )
        Text(name, style = HoopTheme.typography.titleLarge, color = HoopTheme.colors.onSurface)
    }
}

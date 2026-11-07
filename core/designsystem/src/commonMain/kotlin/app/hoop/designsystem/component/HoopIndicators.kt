package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

/** Segmented progress bar of a multi-step flow. */
@Composable
fun HoopProgressSegments(total: Int, filled: Int, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.xs),
    ) {
        repeat(total) { index ->
            Box(
                Modifier.weight(1f).height(HoopTheme.sizes.progressSegmentHeight).clip(HoopTheme.shapes.pill)
                    .background(if (index < filled) HoopTheme.colors.brand else HoopTheme.colors.track),
            )
        }
    }
}

/** Password strength meter: [level] of [total] bars filled with [color], plus a caption. */
@Composable
fun HoopStrengthMeter(level: Int, total: Int, color: Color, caption: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.xs)) {
            repeat(total) { index -> Bar(if (index < level) color else HoopTheme.colors.track) }
        }
        Text(caption, Modifier.padding(top = HoopTheme.spacing.xs), style = HoopTheme.typography.caption, color = HoopTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun RowScope.Bar(color: Color) {
    Box(Modifier.weight(1f).height(HoopTheme.sizes.strengthBarHeight).clip(HoopTheme.shapes.pill).background(color))
}

/** Inline error banner with an alert icon. */
@Composable
fun HoopErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(HoopTheme.shapes.banner).background(HoopTheme.colors.errorContainer)
            .padding(horizontal = HoopTheme.spacing.md - HoopTheme.spacing.xxs, vertical = HoopTheme.spacing.sm + HoopTheme.spacing.xxs)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm),
    ) {
        Icon(HoopIcons.Alert, null, tint = HoopTheme.colors.onErrorContainer, modifier = Modifier.size(HoopTheme.sizes.bannerIcon))
        Text(message, style = HoopTheme.typography.labelMedium, color = HoopTheme.colors.onErrorContainer)
    }
}

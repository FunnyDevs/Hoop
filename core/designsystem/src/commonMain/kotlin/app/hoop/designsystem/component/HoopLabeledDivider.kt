package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.hoop.designsystem.theme.HoopTheme

/** Divider line with a label in the middle, for example "or continue with". */
@Composable
fun HoopLabeledDivider(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm + HoopTheme.spacing.xxs),
    ) {
        Line()
        Text(label, style = HoopTheme.typography.labelMedium, color = HoopTheme.colors.onSurfaceVariant)
        Line()
    }
}

@Composable
private fun RowScope.Line() {
    Box(Modifier.weight(1f).height(HoopTheme.sizes.dividerThickness).background(HoopTheme.colors.divider))
}

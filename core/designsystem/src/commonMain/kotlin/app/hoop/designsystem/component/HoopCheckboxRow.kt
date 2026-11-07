package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

/** Checkbox followed by free content (typically text with links). */
@Composable
fun HoopCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = HoopTheme.colors
    Row(
        modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm),
    ) {
        Box(
            Modifier.size(HoopTheme.sizes.checkboxSize)
                .background(if (checked) colors.brand else if (isError) colors.errorFieldContainer else colors.surface, HoopTheme.shapes.checkbox)
                .border(
                    HoopTheme.sizes.borderWidth,
                    if (checked) colors.brand else if (isError) colors.errorOutline else colors.outlineStrong,
                    HoopTheme.shapes.checkbox,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(HoopIcons.Check, null, tint = colors.onBrand, modifier = Modifier.size(CHECK_GLYPH))
        }
        content()
    }
}

private val CHECK_GLYPH = 16.dp

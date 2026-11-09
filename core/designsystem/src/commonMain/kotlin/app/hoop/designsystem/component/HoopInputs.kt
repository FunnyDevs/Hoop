package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.theme.HoopTheme

/** Rounded input with a send button, used to reply to a post. */
@Composable
fun HoopInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    sendLabel: String,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            modifier = Modifier.weight(1f).height(HoopTheme.sizes.buttonHeight - 10.dp)
                .background(colors.fieldContainer, HoopTheme.shapes.pill).border(HoopTheme.sizes.borderWidth, colors.outlineStrong, HoopTheme.shapes.pill),
            textStyle = HoopTheme.typography.bodyMedium.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().height(HoopTheme.sizes.buttonHeight - 10.dp).padding(horizontal = HoopTheme.spacing.md), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = HoopTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    inner()
                }
            },
        )
        Text(
            sendLabel,
            Modifier.height(HoopTheme.sizes.buttonHeight - 10.dp).background(colors.brand, HoopTheme.shapes.pill)
                .clickable(role = Role.Button, onClick = onSend).padding(horizontal = HoopTheme.spacing.md + 2.dp, vertical = HoopTheme.spacing.sm + 2.dp),
            style = HoopTheme.typography.labelMedium, color = colors.onBrand,
        )
    }
}

/** Multi-line text area with an error state. */
@Composable
fun HoopTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = HoopTheme.colors
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(96.dp)
            .background(if (isError) colors.errorFieldContainer else colors.fieldContainer, HoopTheme.shapes.field)
            .border(HoopTheme.sizes.borderWidth, if (isError) colors.errorOutline else colors.outlineStrong, HoopTheme.shapes.field),
        textStyle = HoopTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.onSurface),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().padding(HoopTheme.spacing.md - 2.dp)) {
                if (value.isEmpty()) Text(placeholder, style = HoopTheme.typography.bodyLarge, color = if (isError) colors.error else colors.onSurfaceVariant)
                inner()
            }
        },
    )
}

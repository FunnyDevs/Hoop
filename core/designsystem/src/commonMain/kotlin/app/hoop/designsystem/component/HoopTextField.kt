package app.hoop.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

enum class HoopFieldState { Default, Error, Success }

/** Defaults of the text field: colors per state, taken from the theme. */
object HoopTextFieldDefaults {
    @Composable
    fun borderColor(state: HoopFieldState, focused: Boolean): Color = when {
        state == HoopFieldState.Error -> HoopTheme.colors.errorOutline
        state == HoopFieldState.Success -> HoopTheme.colors.success
        focused -> HoopTheme.colors.brand
        else -> HoopTheme.colors.outlineStrong
    }

    @Composable
    fun containerColor(state: HoopFieldState, focused: Boolean): Color = when {
        state == HoopFieldState.Error -> HoopTheme.colors.errorFieldContainer
        focused || state == HoopFieldState.Success -> HoopTheme.colors.surface
        else -> HoopTheme.colors.fieldContainer
    }
}

/**
 * Labelled text field. [trailing] is an optional action slot (for example the show-password button);
 * [supportingText] appears below the field and is red when the state is [HoopFieldState.Error].
 */
@Composable
fun HoopTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    state: HoopFieldState = HoopFieldState.Default,
    supportingText: String? = null,
    successContentDescription: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val spacing = HoopTheme.spacing
    Column(modifier.fillMaxWidth()) {
        Text(label, style = HoopTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = HoopTheme.colors.onSurface)
        Box(Modifier.padding(top = spacing.xs)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HoopTheme.sizes.fieldHeight)
                    .background(HoopTextFieldDefaults.containerColor(state, focused), HoopTheme.shapes.field)
                    .border(HoopTheme.sizes.borderWidth, HoopTextFieldDefaults.borderColor(state, focused), HoopTheme.shapes.field),
                singleLine = true,
                textStyle = HoopTheme.typography.bodyLarge.copy(color = HoopTheme.colors.onSurface),
                cursorBrush = SolidColor(HoopTheme.colors.onSurface),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                interactionSource = interaction,
                decorationBox = { inner ->
                    Row(Modifier.fillMaxWidth().height(HoopTheme.sizes.fieldHeight).padding(start = spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(placeholder, style = HoopTheme.typography.bodyLarge, color = HoopTheme.colors.onSurfaceVariant)
                            }
                            inner()
                        }
                        if (trailing != null) {
                            Box(Modifier.padding(end = spacing.xs).height(HoopTheme.sizes.touchTarget)) { trailing() }
                        } else if (state == HoopFieldState.Success) {
                            Icon(
                                HoopIcons.Check,
                                contentDescription = null,
                                tint = HoopTheme.colors.success,
                                modifier = Modifier.padding(end = spacing.md).semantics { successContentDescription?.let { contentDescription = it } },
                            )
                        }
                    }
                },
            )
        }
        if (supportingText != null) {
            val color = when (state) {
                HoopFieldState.Error -> HoopTheme.colors.error
                HoopFieldState.Success -> HoopTheme.colors.successText
                HoopFieldState.Default -> HoopTheme.colors.onSurfaceVariant
            }
            Text(supportingText, Modifier.padding(start = spacing.xxs, top = spacing.xs + spacing.xxs), style = HoopTheme.typography.labelMedium, color = color)
        }
    }
}

/** Password field with a show/hide button. Descriptions for the button come from the caller. */
@Composable
fun HoopPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    showDescription: String,
    hideDescription: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    state: HoopFieldState = HoopFieldState.Default,
    supportingText: String? = null,
    successContentDescription: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
) {
    HoopTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        state = state,
        supportingText = supportingText,
        successContentDescription = successContentDescription,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailing = {
            HoopIconButton(
                icon = if (visible) HoopIcons.EyeOff else HoopIcons.Eye,
                contentDescription = if (visible) hideDescription else showDescription,
                onClick = { onVisibilityChange(!visible) },
                style = HoopIconButtonStyle.Plain,
            )
        },
    )
}

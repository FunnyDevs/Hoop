package app.hoop.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import app.hoop.designsystem.theme.HoopTheme

enum class HoopButtonStyle { Primary, Secondary, Plain }

/** Button defaults, in the style of Compose `*Defaults` objects. */
object HoopButtonDefaults {
    const val DISABLED_ALPHA = 0.5f
    val SpinnerStroke = 2.5.dp

    @Composable
    fun containerColor(style: HoopButtonStyle): Color = when (style) {
        HoopButtonStyle.Primary -> HoopTheme.colors.brand
        HoopButtonStyle.Secondary -> HoopTheme.colors.surface
        HoopButtonStyle.Plain -> HoopTheme.colors.transparent
    }

    @Composable
    fun contentColor(style: HoopButtonStyle): Color = when (style) {
        HoopButtonStyle.Primary -> HoopTheme.colors.onBrand
        HoopButtonStyle.Secondary -> HoopTheme.colors.onSurface
        HoopButtonStyle.Plain -> HoopTheme.colors.onSurfaceVariant
    }

    @Composable
    fun border(style: HoopButtonStyle): BorderStroke? = when (style) {
        HoopButtonStyle.Primary, HoopButtonStyle.Plain -> null
        HoopButtonStyle.Secondary -> BorderStroke(HoopTheme.sizes.borderWidth, HoopTheme.colors.outlineStrong)
    }
}

/** Full-width pill button. */
@Composable
fun HoopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HoopButtonStyle = HoopButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val border = HoopButtonDefaults.border(style)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HoopTheme.sizes.buttonHeight)
            .alpha(if (enabled) 1f else HoopButtonDefaults.DISABLED_ALPHA)
            .clip(HoopTheme.shapes.pill)
            .background(HoopButtonDefaults.containerColor(style))
            .then(if (border != null) Modifier.border(border, HoopTheme.shapes.pill) else Modifier)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(HoopTheme.sizes.iconSmall),
                color = HoopButtonDefaults.contentColor(style),
                strokeWidth = HoopButtonDefaults.SpinnerStroke,
            )
        } else {
            Text(
                text = text,
                style = HoopTheme.typography.labelLarge,
                color = HoopButtonDefaults.contentColor(style),
                textAlign = TextAlign.Center,
            )
        }
    }
}

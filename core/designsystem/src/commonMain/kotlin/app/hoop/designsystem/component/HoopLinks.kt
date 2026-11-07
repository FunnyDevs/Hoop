package app.hoop.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import app.hoop.designsystem.theme.HoopTheme

/** Style of inline links: link color, semi-bold, no underline. */
@Composable
fun hoopLinkStyle(): TextLinkStyles =
    TextLinkStyles(SpanStyle(color = HoopTheme.colors.link, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.None))

/** Builds a text made of [parts]; parts with a non-null `onClick` become links. */
@Composable
fun hoopLinkedText(vararg parts: Pair<String, (() -> Unit)?>): AnnotatedString {
    val style = hoopLinkStyle()
    return buildAnnotatedString {
        parts.forEach { (text, onClick) ->
            if (onClick == null) append(text)
            else withLink(LinkAnnotation.Clickable(tag = text, styles = style, linkInteractionListener = { onClick() })) { append(text) }
        }
    }
}

/** A prompt followed by a link, centered, for example "Don't have an account? Sign up". */
@Composable
fun HoopFooterPrompt(prompt: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = hoopLinkedText("$prompt " to null, action to onClick),
        modifier = modifier.fillMaxWidth().padding(vertical = HoopTheme.spacing.sm),
        style = HoopTheme.typography.bodyMedium,
        color = HoopTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Stand-alone link-styled text button (for example "Forgot password?"). */
@Composable
fun HoopTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = hoopLinkedText(text to onClick),
        modifier = modifier.padding(vertical = HoopTheme.spacing.sm),
        style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = HoopTheme.colors.link,
    )
}

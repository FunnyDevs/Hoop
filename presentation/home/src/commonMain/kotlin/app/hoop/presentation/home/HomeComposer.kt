package app.hoop.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.HoopPlayGlyph
import app.hoop.designsystem.component.HoopTagChip
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.post.PostType
import app.hoop.presentation.home.resources.*
import org.jetbrains.compose.resources.stringResource

private object ComposeSpec {
    val barHeight = 60.dp
    val barCorner = 30.dp
    val barInset = 16.dp
    val barBottom = 28.dp
    val typeButton = 42.dp
    val sendButton = 48.dp
    val sheetHeight = 244.dp
    val sheetCorner = 30.dp
    val sheetTypeButton = 38.dp
    val sheetSend = 52.dp
    val avatar = 34.dp
    val thumb = 44.dp
    const val MORPH_DAMPING = 0.85f
    const val MORPH_ICON_FADE = 2.5f
    const val MORPH_ICON_DEGREES = 90f
    const val MORPH_CONTENT_START = 0.55f
    const val SCRIM_ALPHA = 0.4f
    const val COMPOSER_SCRIM_ALPHA = 0.45f
}

/** Dimming layer behind the bar and the composer. */
@Composable
internal fun ComposeScrim(onClick: () -> Unit, composerOpen: Boolean) {
    Box(
        Modifier.fillMaxSize().background(HoopTheme.colors.brand.copy(alpha = if (composerOpen) ComposeSpec.COMPOSER_SCRIM_ALPHA else ComposeSpec.SCRIM_ALPHA))
            .clickable(onClick = onClick),
    )
}

/**
 * The "+" button and the ask bar as one shape: while [open] grows from the round button into the pill
 * (width, height and color interpolate with a spring) and the bar content fades in once there is room.
 * Tap the bar to write a question; in the initial Hoop it also offers photo and video.
 */
@Composable
internal fun AskBarMorph(
    open: Boolean, fullWidth: Dp, hoopName: String?, isHome: Boolean,
    onToggle: () -> Unit, onOpen: (PostType) -> Unit, modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    val progress by animateFloatAsState(
        if (open) 1f else 0f,
        spring(dampingRatio = ComposeSpec.MORPH_DAMPING, stiffness = Spring.StiffnessMediumLow),
    )
    val p = progress.coerceIn(0f, 1f)
    val fab = HoopTheme.sizes.fab
    val width = lerp(fab, fullWidth, p)
    val height = lerp(fab, ComposeSpec.barHeight, p)
    val shape = RoundedCornerShape(height / 2)
    val newPost = stringResource(Res.string.action_new_post)
    Box(
        modifier.size(width, height)
            .shadow(HoopTheme.elevation.card, shape)
            .background(lerp(colors.accent, colors.surface, p), shape)
            .clip(shape)
            .semantics { contentDescription = newPost }
            .clickable(role = Role.Button) { if (open) onOpen(PostType.Question) else onToggle() },
    ) {
        Icon(
            HoopIcons.Plus, null, tint = colors.onBrand,
            modifier = Modifier.align(Alignment.Center).size(HoopTheme.sizes.iconMedium + 2.dp)
                .alpha((1f - p * ComposeSpec.MORPH_ICON_FADE).coerceIn(0f, 1f)).rotate(ComposeSpec.MORPH_ICON_DEGREES * p),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).requiredWidth(fullWidth).height(ComposeSpec.barHeight)
                .alpha(((p - ComposeSpec.MORPH_CONTENT_START) / (1f - ComposeSpec.MORPH_CONTENT_START)).coerceIn(0f, 1f)),
        ) { AskBarContent(hoopName, isHome, onOpen) }
    }
}

@Composable
private fun AskBarContent(hoopName: String?, isHome: Boolean, onOpen: (PostType) -> Unit) {
    val colors = HoopTheme.colors
    Row(
        Modifier.fillMaxSize().padding(start = 22.dp, end = HoopTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.xs),
    ) {
        Text(
            if (isHome) stringResource(Res.string.ask_placeholder_home) else stringResource(Res.string.ask_placeholder_other, hoopName.orEmpty()),
            Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = HoopTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = colors.onSurfaceVariant,
        )
        if (isHome) {
            RoundAction(colors.tagPhoto, ComposeSpec.typeButton, stringResource(Res.string.action_photo), { onOpen(PostType.Photo) }) {
                Icon(HoopIcons.Camera, null, tint = colors.onTagPhoto, modifier = Modifier.size(HoopTheme.sizes.iconSmall))
            }
            RoundAction(colors.tagVideo, ComposeSpec.typeButton, stringResource(Res.string.action_video), { onOpen(PostType.Video) }) {
                HoopPlayGlyph(colors.onTagVideo, Modifier.size(HoopTheme.sizes.playGlyph + 2.dp))
            }
        }
        RoundAction(colors.accent, ComposeSpec.sendButton, stringResource(Res.string.action_send), { onOpen(PostType.Question) }) {
            Icon(HoopIcons.Send, null, tint = colors.onBrand, modifier = Modifier.size(HoopTheme.sizes.iconMedium - 2.dp))
        }
    }
}

@Composable
private fun RoundAction(background: Color, size: Dp, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size).background(background, HoopTheme.shapes.circle).clip(HoopTheme.shapes.circle)
            .semantics { contentDescription = description }.clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * The writing sheet above the keyboard: who and where, the type with a way to change it,
 * the text in the serif, and the send button.
 */
@Composable
internal fun ComposerSheet(composer: ComposerUi, hoop: HoopUi?, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val spacing = HoopTheme.spacing
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hoopName = hoop?.name ?: stringResource(Res.string.hoop_home_name)
    val kind = composer.type.toKind()
    Column(modifier.fillMaxSize().imePadding(), verticalArrangement = Arrangement.Bottom) {
        Box(
            Modifier.fillMaxWidth().height(ComposeSpec.sheetHeight)
                .background(colors.surface, RoundedCornerShape(topStart = ComposeSpec.sheetCorner, topEnd = ComposeSpec.sheetCorner))
                .padding(start = spacing.lg - 2.dp, end = spacing.lg - 2.dp, top = spacing.lg - 4.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Box(Modifier.size(ComposeSpec.avatar).background(colors.avatar, HoopTheme.shapes.circle), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.user_initials), style = HoopTheme.typography.labelMedium, color = colors.onSurface)
                    }
                    Text(stringResource(Res.string.composer_in, hoopName), style = HoopTheme.typography.labelMedium.copy(fontSize = HoopTheme.typography.bodyMedium.fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.onSurface)
                    HoopTagChip(kind, tagLabel(composer.type))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.End)) {
                        if (hoop?.isHome == true) {
                            if (composer.type != PostType.Photo) RoundAction(colors.tagPhoto, ComposeSpec.sheetTypeButton, stringResource(Res.string.action_photo), { onIntent(HomeIntent.ComposerTypeChanged(PostType.Photo)) }) {
                                Icon(HoopIcons.Camera, null, tint = colors.onTagPhoto, modifier = Modifier.size(HoopTheme.sizes.iconSmall - 2.dp))
                            }
                            if (composer.type != PostType.Video) RoundAction(colors.tagVideo, ComposeSpec.sheetTypeButton, stringResource(Res.string.action_video), { onIntent(HomeIntent.ComposerTypeChanged(PostType.Video)) }) {
                                HoopPlayGlyph(colors.onTagVideo, Modifier.size(HoopTheme.sizes.playGlyph))
                            }
                            if (composer.type != PostType.Question) RoundAction(colors.tagQuestion, ComposeSpec.sheetTypeButton, stringResource(Res.string.menu_question), { onIntent(HomeIntent.ComposerTypeChanged(PostType.Question)) }) {
                                Text(stringResource(Res.string.glyph_question), style = HoopTheme.typography.titleMedium, color = colors.onTagQuestion)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.md + 2.dp))
                BasicTextField(
                    value = composer.text, onValueChange = { onIntent(HomeIntent.ComposerTextChanged(it)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    textStyle = HoopTheme.typography.titleLarge.copy(fontSize = HoopTheme.typography.titleLarge.fontSize * 1.35f, lineHeight = HoopTheme.typography.titleLarge.fontSize * 1.55f, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { inner ->
                        Box {
                            if (composer.text.isEmpty()) {
                                Text(
                                    if (composer.showError) stringResource(Res.string.composer_empty_error)
                                    else stringResource(if (composer.type == PostType.Question) Res.string.composer_hint_question else Res.string.composer_hint_media),
                                    style = HoopTheme.typography.titleLarge.copy(fontSize = HoopTheme.typography.titleLarge.fontSize * 1.35f, lineHeight = HoopTheme.typography.titleLarge.fontSize * 1.55f),
                                    color = if (composer.showError) colors.error else colors.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            if (composer.type != PostType.Question) {
                Row(Modifier.align(Alignment.BottomStart).padding(bottom = spacing.md - 2.dp), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SampleMedia.pickerKeys.forEachIndexed { index, key ->
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier.size(ComposeSpec.thumb).clip(shape)
                                .border(3.dp, if (index == composer.mediaIndex) colors.accent else Color.Transparent, shape)
                                .clickable { onIntent(HomeIntent.ComposerMediaPicked(index)) },
                        ) {
                            mediaPainter(key)?.let { Image(it, stringResource(Res.string.composer_media_option, index + 1), Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        }
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(bottom = spacing.md - 2.dp).size(ComposeSpec.sheetSend)
                    .shadow(HoopTheme.elevation.badge, HoopTheme.shapes.circle).background(colors.accent, HoopTheme.shapes.circle)
                    .clip(HoopTheme.shapes.circle).semantics { contentDescription = "" }
                    .clickable(role = Role.Button) { onIntent(HomeIntent.ComposerPublished) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(HoopIcons.Send, stringResource(Res.string.action_send), tint = colors.onBrand, modifier = Modifier.size(HoopTheme.sizes.iconMedium - 2.dp))
            }
        }
    }
}

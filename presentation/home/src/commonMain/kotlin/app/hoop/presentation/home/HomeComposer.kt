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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.offset
import app.hoop.presentation.home.camera.isVideo
import app.hoop.presentation.home.camera.rememberMediaBitmap
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
    val sheetSend = 52.dp
    val avatar = 34.dp
    val thumb = 72.dp
    val thumbCorner = 18.dp
    val thumbBorder = 3.dp
    val thumbBottom = 18.dp
    val thumbPlay = 24.dp
    val retake = 26.dp
    val retakeIcon = 13.dp
    val retakeOverhang = 8.dp
    const val MORPH_DAMPING = 0.85f
    const val MORPH_ICON_FADE = 2.5f
    const val MORPH_ICON_DEGREES = 90f
    const val MORPH_CONTENT_START = 0.55f
    const val MORPH_BAR_FADE = 3f
    const val MORPH_SHEET_START = 0.4f
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

/** The three shapes of the one surface: the round button, the ask bar, the writing sheet. */
internal object ComposeStage {
    const val BUTTON = 0
    const val BAR = 1
    const val SHEET = 2
}

/**
 * One surface that is the "+" button, the ask bar and the writing sheet: [stage] moves it between them and its width,
 * height, position, corners and color interpolate with a spring while the content of each shape fades in and out.
 * The sheet sits above the keyboard, so closing the keyboard folds it back into the bar.
 */
@Composable
internal fun ComposeMorph(
    stage: Int, screenWidth: Dp, composer: ComposerUi?, hoop: HoopUi?,
    onToggle: () -> Unit, onOpen: (PostType) -> Unit, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier,
) {
    val colors = HoopTheme.colors
    val density = LocalDensity.current
    val progress by animateFloatAsState(
        stage.toFloat(),
        spring(dampingRatio = ComposeSpec.MORPH_DAMPING, stiffness = Spring.StiffnessMediumLow),
    )
    val p = progress.coerceIn(0f, ComposeStage.SHEET.toFloat())
    val bar = p.coerceAtMost(1f)
    val sheet = (p - 1f).coerceIn(0f, 1f)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val fab = HoopTheme.sizes.fab
    val barWidth = screenWidth - ComposeSpec.barInset * 2
    val width = if (sheet > 0f) lerp(barWidth, screenWidth, sheet) else lerp(fab, barWidth, bar)
    val height = if (sheet > 0f) lerp(ComposeSpec.barHeight, ComposeSpec.sheetHeight, sheet) else lerp(fab, ComposeSpec.barHeight, bar)
    val endInset = lerp(ComposeSpec.barInset, 0.dp, sheet)
    val bottomInset = lerp(ComposeSpec.barBottom + navBottom, maxOf(imeBottom, navBottom), sheet)
    val topRadius = if (sheet > 0f) ComposeSpec.barCorner else height / 2
    val bottomRadius = if (sheet > 0f) lerp(ComposeSpec.barCorner, 0.dp, sheet) else height / 2
    val shape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius)
    val newPost = stringResource(Res.string.action_new_post)

    // Keep the sheet content while it folds back, after the composer itself is gone.
    var lastComposer by remember { mutableStateOf(composer) }
    if (composer != null) lastComposer = composer
    LaunchedEffect(sheet, composer) { if (sheet == 0f && composer == null) lastComposer = null }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = endInset, bottom = bottomInset).size(width, height)
                .shadow(HoopTheme.elevation.card, shape)
                .background(lerp(colors.accent, colors.surface, bar), shape)
                .clip(shape)
                .semantics { contentDescription = newPost }
                .clickable(enabled = stage < ComposeStage.SHEET, role = Role.Button) {
                    if (stage == ComposeStage.BAR) onOpen(PostType.Question) else onToggle()
                },
        ) {
            Icon(
                HoopIcons.Plus, null, tint = colors.onBrand,
                modifier = Modifier.align(Alignment.Center).size(HoopTheme.sizes.iconMedium + 2.dp)
                    .alpha((1f - bar * ComposeSpec.MORPH_ICON_FADE).coerceIn(0f, 1f)).rotate(ComposeSpec.MORPH_ICON_DEGREES * bar),
            )
            val barAlpha = ((bar - ComposeSpec.MORPH_CONTENT_START) / (1f - ComposeSpec.MORPH_CONTENT_START)).coerceIn(0f, 1f) *
                (1f - sheet * ComposeSpec.MORPH_BAR_FADE).coerceIn(0f, 1f)
            if (barAlpha > 0f) {
                Box(Modifier.align(Alignment.TopEnd).requiredWidth(barWidth).height(ComposeSpec.barHeight).alpha(barAlpha)) {
                    AskBarContent(hoop?.name, hoop?.isHome == true, onOpen) { onIntent(HomeIntent.CameraRequested) }
                }
            }
            val sheetAlpha = ((sheet - ComposeSpec.MORPH_SHEET_START) / (1f - ComposeSpec.MORPH_SHEET_START)).coerceIn(0f, 1f)
            val shown = lastComposer
            if (shown != null && sheet > 0f) {
                Box(Modifier.align(Alignment.TopStart).requiredSize(screenWidth, ComposeSpec.sheetHeight).alpha(sheetAlpha)) {
                    ComposerContent(shown, hoop, onIntent)
                }
            }
        }
    }
}

@Composable
private fun AskBarContent(hoopName: String?, isHome: Boolean, onOpen: (PostType) -> Unit, onCamera: () -> Unit) {
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
        // One camera button, only in Home: photos and videos are taken live there (a tap for a photo, hold for a video).
        if (isHome) {
            RoundAction(colors.tagPhoto, ComposeSpec.typeButton, stringResource(Res.string.action_camera), onCamera) {
                Icon(HoopIcons.Camera, null, tint = colors.onTagPhoto, modifier = Modifier.size(HoopTheme.sizes.iconSmall))
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
 * What the writing sheet shows: who and where, the type with a way to change it,
 * the text in the serif, and the send button. The surface around it is [ComposeMorph].
 */
@Composable
internal fun ComposerContent(composer: ComposerUi, hoop: HoopUi?, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val spacing = HoopTheme.spacing
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hoopName = hoop?.name ?: stringResource(Res.string.hoop_home_name)
    val kind = composer.type.toKind()
    val retakeLabel = stringResource(Res.string.composer_retake)
    run {
        Box(modifier.fillMaxSize().padding(start = spacing.lg - 2.dp, end = spacing.lg - 2.dp, top = spacing.lg - 4.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Box(Modifier.size(ComposeSpec.avatar).background(colors.avatar, HoopTheme.shapes.circle), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.user_initials), style = HoopTheme.typography.labelMedium, color = colors.onSurface)
                    }
                    Text(stringResource(Res.string.composer_in, hoopName), style = HoopTheme.typography.labelMedium.copy(fontSize = HoopTheme.typography.bodyMedium.fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.onSurface)
                    HoopTagChip(kind, tagLabel(composer.type))
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
            // The one shot taken live, with a small button to take it again (it reopens the camera).
            composer.media?.let { media ->
                val shape = RoundedCornerShape(ComposeSpec.thumbCorner)
                Box(Modifier.align(Alignment.BottomStart).padding(bottom = ComposeSpec.thumbBottom).size(ComposeSpec.thumb)) {
                    Box(Modifier.fillMaxSize().clip(shape).background(colors.neutralContainer).border(ComposeSpec.thumbBorder, colors.accent, shape)) {
                        rememberMediaBitmap(media.uri)?.let {
                            Image(it, stringResource(Res.string.composer_media_thumb), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        if (media.isVideo) HoopPlayGlyph(colors.onScrim, Modifier.align(Alignment.Center).size(ComposeSpec.thumbPlay))
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(ComposeSpec.retakeOverhang, -ComposeSpec.retakeOverhang).size(ComposeSpec.retake)
                            .background(colors.brand, HoopTheme.shapes.circle).border(2.dp, colors.surface, HoopTheme.shapes.circle).clip(HoopTheme.shapes.circle)
                            .semantics { contentDescription = retakeLabel }.clickable(role = Role.Button) { onIntent(HomeIntent.CameraRequested) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(HoopIcons.Retake, null, tint = colors.onBrand, modifier = Modifier.size(ComposeSpec.retakeIcon))
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

package app.hoop.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme

enum class HoopPostKind { Question, Photo, Video }

object HoopPostCardDefaults {
    val CarouselWidth: Dp = 200.dp
    val CarouselHeight: Dp = 220.dp
    val CornerRadius: Dp = 30.dp
    val ExpandedCornerRadius: Dp = 0.dp
    val TopScrimHeight: Dp = 64.dp
    const val TOP_SCRIM_ALPHA = 0.32f
    const val BOTTOM_SCRIM_ALPHA = 0.78f
    const val BOTTOM_SCRIM_FRACTION = 0.7f
    const val CLOSED_TITLE_LINES = 3
    const val OPEN_TITLE_LINES = 6
    /** Below this progress the card still counts as closed (title limited to [CLOSED_TITLE_LINES]). */
    const val CLOSED_PROGRESS = 0.05f
    /** Pills are tappable only once the card is (almost) fully open. */
    const val PILLS_ACTIVE_PROGRESS = 0.95f
    const val PILL_OFF_ALPHA = 0.7f

    // Values interpolated by the opening progress: closed -> open.
    val AvatarInsetClosed: Dp = 12.dp
    val AvatarInsetStartOpen: Dp = 16.dp
    val AvatarTopOpen: Dp = 52.dp
    val AvatarSizeClosed: Dp = 34.dp
    val AvatarSizeOpen: Dp = 40.dp
    val AuthorGapOpen: Dp = 10.dp
    val AuthorMaxWidthOpen: Dp = 300.dp
    val TitleInsetClosed: Dp = 14.dp
    val TitleInsetOpen: Dp = 24.dp
    val TitleBottomClosed: Dp = 36.dp
    val TitleBottomOpen: Dp = 182.dp
    val TitleSizeClosed = 20.sp
    val TitleSizeOpen = 42.sp
    const val TITLE_LINE_HEIGHT_CLOSED = 1.12f
    const val TITLE_LINE_HEIGHT_OPEN = 1.1f
    val MetaBottomClosed: Dp = 14.dp
    val MetaIconSize: Dp = 16.dp
    val MetaItemGap: Dp = 10.dp
    val StreetBottomClosed: Dp = 14.dp
    val StreetBottomOpen: Dp = 150.dp
    val PillsBottom: Dp = 64.dp
    val PillsRise: Dp = 14.dp
    /** The street row hangs this far below the bottom of the title, so the two never overlap while opening. */
    val StreetBelowTitle: Dp = 32.dp
    val PillHeight: Dp = 46.dp
    val PillIconSize: Dp = 24.dp
    val PillsInset: Dp = 16.dp
    val PillsGap: Dp = 10.dp
    val VideoBarBottom: Dp = 124.dp
    val VideoBarHeight: Dp = 3.dp
    const val VIDEO_BAR_PLAYED = 0.32f

    // Progress ranges (start, end) in which each part appears or disappears.
    val MetaFade = 0f to 0.3f
    /** Closed counters shrink away over this range, slower than the text next to them. */
    val CounterScale = 0f to 0.5f
    val NameFade = 0.35f to 0.7f
    val StreetFade = 0.55f to 0.85f
    val PillsFade = 0.45f to 1f
    const val OVERSHOOT_MAX = 1.08f

    // Counter-to-pill chips.
    val ChipHeightClosed: Dp = 18.dp
    /** Width kept free for the two counters (up to four characters each) next to the time on the closed card. */
    val CountersReserve: Dp = 112.dp
    val MetaMinTextWidth: Dp = 40.dp
    val ChipIconClosed: Dp = 16.dp
    val ChipIconGapClosed: Dp = 4.dp
    val ChipIconGapOpen: Dp = 8.dp
    val ChipClosedPadding: Dp = 2.dp
    val ChipFontClosed = 12.sp
    val ChipFontOpen = 15.sp
    const val ChipBoldFrom = 0.5f
    val ChipBackground = 0.2f to 0.65f
    val ChipToneFade = 0.35f to 0.55f
    val ChipIconSwap = 0.4f to 0.6f
    val ChipLabel = 0.55f to 0.9f
    val ChipLabelGapOpen: Dp = 6.dp
    val ChipLabelMaxWidth: Dp = 90.dp
    val TopScrimFade = 0.2f to 0.7f

    // Comments sheet: how the pills react while it rises (ranges of the sheet progress).
    val SheetFlyFade = 0f to 0.3f
    val SheetLabelFade = 0.1f to 0.4f
    val SheetFlyDrop: Dp = 20.dp
    /** Landscape view: the post's elements fade out over this range of the immersion progress. */
    val ImmersionFade = 0f to 0.3f
}

/** 0 before [range].first, 1 after [range].second, linear in between. */
fun rampOf(progress: Float, range: Pair<Float, Float>): Float =
    ((progress - range.first) / (range.second - range.first)).coerceIn(0f, 1f)

/**
 * A post card used in the carousel and, with [progress] going from 0 to 1, expanded to full screen. Closed it shows only the
 * author's avatar, the title and a row of distance, time and counters. Opening moves the avatar, grows the title, reveals the
 * author's name, the street and two pills (Ci volo, replies); every part follows the ranges in [HoopPostCardDefaults].
 * Text and images come from the caller.
 */
@Composable
fun HoopPostCard(
    kind: HoopPostKind,
    title: String,
    fliesCount: String,
    repliesCount: String,
    authorInitials: String,
    authorHandle: String,
    ageLabel: String,
    streetLabel: String,
    fliesLabel: String,
    repliesLabel: String,
    flown: Boolean,
    onFlyClick: () -> Unit,
    onRepliesClick: () -> Unit,
    modifier: Modifier = Modifier,
    questionColor: Color = HoopTheme.colors.questionCards.first(),
    media: Painter? = null,
    progress: Float = 0f,
    /** Space taken by the system navigation bar at the bottom: the open post's bottom items rise by it. */
    bottomInset: Dp = 0.dp,
    /** Progress (0..1) of the comments sheet rising over the post; read in the draw phase only. */
    sheetProgress: () -> Float = { 0f },
    /** Progress (0..1) of the full-screen landscape view of a horizontal photo or video; read in the layout and draw phases only. */
    immersion: () -> Float = { 0f },
    /** +1 or -1: the media turns against the phone's physical rotation so it stays upright for the viewer. */
    immersionSign: Float = 1f,
) {
    val colors = HoopTheme.colors
    val d = HoopPostCardDefaults
    val p = progress.coerceIn(0f, 1f)
    // Geometry may overshoot a little with the spring; opacities and widths stay within their range.
    val k = progress.coerceIn(0f, d.OVERSHOOT_MAX)
    fun mix(a: Dp, b: Dp) = a + (b - a) * p
    val lift = bottomInset * p
    val isQuestion = kind == HoopPostKind.Question
    val hasMedia = !isQuestion && media != null
    val contentColor = if (hasMedia) colors.onScrim else colors.onSurface
    val fade = (1f - p * 1.6f).coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cardWidth = maxWidth
        if (!hasMedia) {
            Box(Modifier.fillMaxSize().background(questionColor))
        } else {
            val aspect = media!!.intrinsicSize.let { if (it.isSpecified && it.height > 0f) it.width / it.height else 1f }
            Box(Modifier.fillMaxSize().clipToBounds()) {
                if (aspect > 1f) {
                    // Horizontal media always covers the card; with [immersion] it also turns and grows so no bar is ever visible.
                    Image(
                        media, null,
                        Modifier.layout { measurable, constraints ->
                            val w0 = constraints.maxWidth.toFloat()
                            val h0 = constraints.maxHeight.toFloat()
                            val theta = immersion() * (PI / 2).toFloat()
                            val sin = abs(sin(theta))
                            val cos = abs(cos(theta))
                            val height = max((w0 * cos + h0 * sin) / aspect, w0 * sin + h0 * cos)
                            val width = height * aspect
                            val placeable = measurable.measure(Constraints.fixed(width.roundToInt(), height.roundToInt()))
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(((w0 - width) / 2f).roundToInt(), ((h0 - height) / 2f).roundToInt())
                            }
                        }.graphicsLayer { rotationZ = immersionSign * 90f * immersion() },
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Image(media, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
        // Everything but the media fades out in the landscape view and comes back when turning upright.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - rampOf(immersion(), d.ImmersionFade) }) {
        if (hasMedia) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(d.BOTTOM_SCRIM_FRACTION)
                    .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = 0f), colors.scrim.copy(alpha = d.BOTTOM_SCRIM_ALPHA)))),
            )
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(d.TopScrimHeight + 40.dp * p)
                    .alpha(rampOf(p, d.TopScrimFade))
                    .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = d.TOP_SCRIM_ALPHA), colors.scrim.copy(alpha = 0f)))),
            )
            if (kind == HoopPostKind.Video) {
                HoopPlayBadge(Modifier.align(Alignment.Center).offset(y = (-HoopTheme.spacing.xl)).alpha(fade))
                // Playback progress, above the pills (there is no player yet, so it is a static indication).
                Box(
                    Modifier.align(Alignment.BottomStart).padding(horizontal = d.PillsInset).padding(bottom = d.VideoBarBottom + bottomInset)
                        .fillMaxWidth().height(d.VideoBarHeight).alpha(rampOf(p, d.PillsFade))
                        .clip(HoopTheme.shapes.pill).background(colors.onScrim.copy(alpha = 0.35f)),
                ) { Box(Modifier.fillMaxWidth(d.VIDEO_BAR_PLAYED).fillMaxHeight().background(colors.onScrim)) }
            }
        }

        // Author: only the avatar when closed; with name and time when open.
        Row(
            Modifier.align(Alignment.TopStart).padding(start = mix(d.AvatarInsetClosed, d.AvatarInsetStartOpen), top = mix(d.AvatarInsetClosed, d.AvatarTopOpen)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(mix(d.AvatarSizeClosed, d.AvatarSizeOpen)).clip(HoopTheme.shapes.circle).background(colors.avatar),
                contentAlignment = Alignment.Center,
            ) { Text(authorInitials, style = HoopTheme.typography.labelSmall, color = colors.onSurface) }
            Column(
                Modifier.padding(start = d.AuthorGapOpen * p).widthIn(max = d.AuthorMaxWidthOpen * p).alpha(rampOf(p, d.NameFade)),
            ) {
                Text(authorHandle, style = HoopTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ageLabel, style = HoopTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Title, anchored at the bottom.
        val titleSize = (d.TitleSizeClosed.value + (d.TitleSizeOpen.value - d.TitleSizeClosed.value) * p).sp
        val lineHeight = d.TITLE_LINE_HEIGHT_CLOSED + (d.TITLE_LINE_HEIGHT_OPEN - d.TITLE_LINE_HEIGHT_CLOSED) * p
        Text(
            title,
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(horizontal = mix(d.TitleInsetClosed, d.TitleInsetOpen)).padding(bottom = mix(d.TitleBottomClosed, d.TitleBottomOpen) + lift),
            style = HoopTheme.typography.titleLarge.copy(fontSize = titleSize, lineHeight = titleSize * lineHeight),
            color = contentColor,
            maxLines = if (p < d.CLOSED_PROGRESS) d.CLOSED_TITLE_LINES else d.OPEN_TITLE_LINES,
            overflow = TextOverflow.Ellipsis,
        )

        // Closed: the time on the left, vertically centered on the same line as the counters. It never runs under them.
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = d.TitleInsetClosed).padding(bottom = d.MetaBottomClosed)
                .height(d.ChipHeightClosed).widthIn(max = (cardWidth - d.TitleInsetClosed * 2 - d.CountersReserve).coerceAtLeast(d.MetaMinTextWidth))
                .alpha(1f - rampOf(p, d.MetaFade)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(ageLabel, style = HoopTheme.typography.caption, color = contentColor.copy(alpha = 0.92f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Open: the street, between the title and the pills.
        Row(
            Modifier.align(Alignment.BottomStart).padding(horizontal = d.TitleInsetOpen).padding(bottom = mix(d.TitleBottomClosed, d.TitleBottomOpen) - d.StreetBelowTitle + lift)
                .alpha(rampOf(p, d.StreetFade)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HoopIcons.Pin, null, tint = contentColor, modifier = Modifier.size(d.MetaIconSize))
            Text(streetLabel, Modifier.padding(start = HoopTheme.spacing.xs), style = HoopTheme.typography.labelMedium.copy(fontSize = 14.sp), color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // The two counters of the closed card ARE the pills of the open post: they widen, grow taller and gain a background.
        val pillWidth = (cardWidth - d.PillsInset * 2 - d.PillsGap) / 2
        val pillsActive = p >= d.PILLS_ACTIVE_PROGRESS
        val chipBottom = d.MetaBottomClosed + (d.PillsBottom - d.MetaBottomClosed) * k + bottomInset * p
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(start = d.TitleInsetClosed, end = mix(d.TitleInsetClosed, d.PillsInset)).padding(bottom = chipBottom),
            verticalAlignment = Alignment.Bottom,
        ) {
            Spacer(Modifier.weight(1f))
            MorphingChip(
                HoopIcons.Fly, HoopIcons.Fly, fliesCount, fliesLabel, p, k, pillWidth, contentColor, hasMedia, flown, pillsActive, onFlyClick,
                // While the comments sheet rises, the Ci volo pill slips down and out.
                extra = Modifier.graphicsLayer {
                    val gone = rampOf(sheetProgress(), d.SheetFlyFade)
                    alpha = 1f - gone
                    translationY = d.SheetFlyDrop.toPx() * gone
                },
            )
            Spacer(Modifier.width(d.MetaItemGap + (d.PillsGap - d.MetaItemGap) * p))
            MorphingChip(
                HoopIcons.Chat, HoopIcons.ArrowUp, repliesCount, repliesLabel, p, k, pillWidth, contentColor, hasMedia, false, pillsActive, onRepliesClick,
                labelFade = { 1f - rampOf(sheetProgress(), d.SheetLabelFade) },
            )
        }
        }
    }
}

/**
 * A counter of the closed card that becomes a pill of the open post. [p] drives opacities and width, [k] (the same value, allowed
 * to overshoot slightly) drives height, font and icon. The closed width is measured from the content; the open one is [pillWidth].
 */
@Composable
private fun MorphingChip(
    closedIcon: ImageVector, openIcon: ImageVector, count: String, label: String, p: Float, k: Float, pillWidth: Dp,
    closedColor: Color, onMedia: Boolean, active: Boolean, enabled: Boolean, onClick: () -> Unit,
    extra: Modifier = Modifier, labelFade: () -> Float = { 1f },
) {
    val colors = HoopTheme.colors
    val d = HoopPostCardDefaults
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val baseStyle = HoopTheme.typography.caption.copy(fontSize = d.ChipFontClosed)
    val closedWidth = remember(count, baseStyle, density) {
        val textPx = measurer.measure(count, baseStyle).size.width
        d.ChipIconClosed + d.ChipIconGapClosed + with(density) { textPx.toDp() } + d.ChipClosedPadding * 2
    }
    fun mix(a: Dp, b: Dp) = a + (b - a) * k
    val height = mix(d.ChipHeightClosed, d.PillHeight)
    val width = closedWidth + (pillWidth - closedWidth) * p
    val background = rampOf(p, d.ChipBackground)
    val container = if (active) colors.accent.copy(alpha = background) else colors.surface.copy(alpha = d.PILL_OFF_ALPHA * background)
    // On media the closed content is white and turns dark with the white pill; questions are always dark.
    val openContent = if (active) colors.onScrim else colors.onSurface
    val content = if (onMedia) lerp(closedColor, openContent, rampOf(p, d.ChipToneFade)) else openContent
    val bold = k > d.ChipBoldFrom
    val labelProgress = rampOf(p, d.ChipLabel)
    val swap = rampOf(p, d.ChipIconSwap)
    val iconSize = mix(d.ChipIconClosed, d.PillIconSize)
    Row(
        extra.width(width).height(height).clip(RoundedCornerShape(height / 2)).background(container)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(iconSize)) {
            Icon(closedIcon, null, tint = content, modifier = Modifier.size(iconSize).alpha(if (openIcon === closedIcon) 1f else 1f - swap))
            if (openIcon !== closedIcon) Icon(openIcon, null, tint = content, modifier = Modifier.size(iconSize).alpha(swap))
        }
        Text(
            count,
            Modifier.padding(start = mix(d.ChipIconGapClosed, d.ChipIconGapOpen)),
            style = baseStyle.copy(fontSize = (d.ChipFontClosed.value + (d.ChipFontOpen.value - d.ChipFontClosed.value) * k).sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
            color = content, maxLines = 1, softWrap = false,
        )
        Text(
            label,
            Modifier.padding(start = d.ChipLabelGapOpen * labelProgress).widthIn(max = d.ChipLabelMaxWidth * labelProgress).graphicsLayer { alpha = labelProgress * labelFade() },
            style = baseStyle.copy(fontSize = (d.ChipFontClosed.value + (d.ChipFontOpen.value - d.ChipFontClosed.value) * k).sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
            color = content, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
        )
    }
}

/** Small colored label telling the post type. */
@Composable
fun HoopTagChip(kind: HoopPostKind, label: String, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val (container, content) = when (kind) {
        HoopPostKind.Question -> colors.tagQuestion to colors.onTagQuestion
        HoopPostKind.Photo -> colors.tagPhoto to colors.onTagPhoto
        HoopPostKind.Video -> colors.tagVideo to colors.onTagVideo
    }
    Text(
        label,
        modifier.clip(HoopTheme.shapes.chip).background(container).padding(horizontal = HoopTheme.spacing.sm - 1.dp, vertical = HoopTheme.spacing.xxs + 1.dp),
        style = HoopTheme.typography.labelSmall, color = content,
    )
}

package app.hoop.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostType
import app.hoop.presentation.home.camera.rememberMediaBitmap
import app.hoop.presentation.home.resources.*
import org.jetbrains.compose.resources.stringResource

/** Measures of the card that shows a post being sent and of the sheet that lists the waiting ones (from the design). */
private object PublishSpec {
    val sideMargin = 16.dp
    val cardHeight = 68.dp
    val cardRadius = 22.dp
    val thumb = 44.dp
    val thumbRadius = 12.dp
    val rowThumb = 52.dp
    val progressHeight = 4.dp
    val contentPadding = 12.dp
    val actionHorizontal = 14.dp
    val actionVertical = 8.dp
    val sheetRadius = 28.dp
    val sheetPadding = 20.dp
    val rowGap = 12.dp
    val closeTarget = 36.dp
    val icon = 20.dp
    const val FULL_PERCENT = 100
}

/**
 * The card at the top of the home for a post in flight: progress while it uploads, the reason when it failed,
 * or the waiting posts when there is no connection. It never covers an open story or the camera.
 */
@Composable
internal fun PublishStatusCard(state: HomeUiState, top: androidx.compose.ui.unit.Dp, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val publishing = state.publishing
    val visible = state.openPostId == null && state.capture == null && (publishing != null || state.queued.isNotEmpty())
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.padding(top = top, start = PublishSpec.sideMargin, end = PublishSpec.sideMargin),
        enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it },
    ) {
        when {
            publishing != null -> PublishingCard(publishing, state, onIntent)
            else -> QueueCard(state, onIntent)
        }
    }
}

@Composable
private fun CardFrame(onClick: (() -> Unit)?, progress: Float?, content: @Composable RowScope.() -> Unit) {
    val colors = HoopTheme.colors
    val shape = RoundedCornerShape(PublishSpec.cardRadius)
    Box(
        Modifier.fillMaxWidth().height(PublishSpec.cardHeight)
            .shadow(HoopTheme.elevation.card, shape).clip(shape).background(colors.surface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = PublishSpec.contentPadding),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PublishSpec.rowGap),
        ) { content() }
        if (progress != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(PublishSpec.progressHeight).background(colors.track)) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(PublishSpec.progressHeight).background(colors.accent))
            }
        }
    }
}

@Composable
private fun PublishingCard(publishing: PublishingUi, state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    val hoopName = hoopName(state, publishing.hoopId)
    val failure = publishing.failure
    if (failure == null) {
        CardFrame(onClick = null, progress = publishing.progress) {
            PostThumbnail(publishing.type, publishing.media, PublishSpec.thumb)
            val percent = "${(publishing.progress * PublishSpec.FULL_PERCENT).toInt()}%"
            CardTexts(stringResource(Res.string.publish_progress, percent), stringResource(Res.string.publish_subtitle, typeLabel(publishing.type), hoopName), Modifier.weight(1f))
            CloseButton(stringResource(Res.string.publish_cancel)) { onIntent(HomeIntent.PublishCancelled) }
        }
    } else {
        CardFrame(onClick = null, progress = null) {
            Box(Modifier.size(PublishSpec.thumb).clip(RoundedCornerShape(PublishSpec.thumbRadius)).background(colors.errorContainer), contentAlignment = Alignment.Center) {
                Icon(HoopIcons.Alert, null, tint = colors.onErrorContainer, modifier = Modifier.size(PublishSpec.icon))
            }
            val (message, action) = when (failure) {
                PublishFailure.TooFar -> stringResource(Res.string.publish_error_too_far) to stringResource(Res.string.publish_error_too_far_action)
                PublishFailure.TooManyRequests -> stringResource(Res.string.publish_error_too_many) to stringResource(Res.string.publish_error_too_many_action)
                PublishFailure.Generic -> stringResource(Res.string.publish_error_generic) to stringResource(Res.string.publish_error_generic_action)
            }
            CardTexts(stringResource(Res.string.publish_failed_title), message, Modifier.weight(1f), subtitleLines = 2)
            ActionPill(action) {
                onIntent(if (failure == PublishFailure.Generic) HomeIntent.PublishRetried else HomeIntent.PublishFailureDismissed)
            }
        }
    }
}

@Composable
private fun QueueCard(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    // The exit animation recomposes this with the already emptied queue, so an empty list is a normal frame.
    val first = state.queued.firstOrNull() ?: return
    if (state.needsSignIn) {
        CardFrame(onClick = { onIntent(HomeIntent.QueueOpened) }, progress = null) {
            Box(Modifier.size(PublishSpec.thumb).clip(RoundedCornerShape(PublishSpec.thumbRadius)).background(colors.errorContainer), contentAlignment = Alignment.Center) {
                Icon(HoopIcons.Alert, null, tint = colors.onErrorContainer, modifier = Modifier.size(PublishSpec.icon))
            }
            CardTexts(stringResource(Res.string.queue_session_title), stringResource(Res.string.queue_session_subtitle), Modifier.weight(1f), subtitleLines = 2)
            ActionPill(stringResource(Res.string.queue_session_action)) { onIntent(HomeIntent.SignInRequested) }
        }
        return
    }
    val many = state.queued.size > 1
    CardFrame(onClick = { onIntent(HomeIntent.QueueOpened) }, progress = null) {
        PostThumbnail(first.type, first.media, PublishSpec.thumb)
        CardTexts(
            if (many) stringResource(Res.string.queue_title_many, state.queued.size) else stringResource(Res.string.queue_title),
            stringResource(if (many) Res.string.queue_subtitle_many else Res.string.queue_subtitle_one),
            Modifier.weight(1f),
        )
        Icon(HoopIcons.CloudOff, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(PublishSpec.icon))
    }
}

@Composable
private fun CardTexts(title: String, subtitle: String, modifier: Modifier, subtitleLines: Int = 1) {
    val colors = HoopTheme.colors
    Column(modifier) {
        Text(title, style = HoopTheme.typography.labelLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = HoopTheme.typography.caption, color = colors.onSurfaceVariant, maxLines = subtitleLines, overflow = TextOverflow.Ellipsis)
    }
}

/** The orange button of a card, always the accent colour. */
@Composable
private fun ActionPill(text: String, onClick: () -> Unit) {
    val colors = HoopTheme.colors
    Text(
        text,
        Modifier.clip(HoopTheme.shapes.pill).background(colors.accent).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = PublishSpec.actionHorizontal, vertical = PublishSpec.actionVertical),
        style = HoopTheme.typography.labelMedium, color = colors.onBrand,
    )
}

@Composable
private fun CloseButton(description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(PublishSpec.closeTarget).clip(HoopTheme.shapes.circle).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(HoopIcons.Close, description, tint = HoopTheme.colors.onSurfaceVariant, modifier = Modifier.size(PublishSpec.icon))
    }
}

/** The picture of a post, or a speech bubble for a question, which has none. */
@Composable
private fun PostThumbnail(type: PostType, media: MediaRef?, size: androidx.compose.ui.unit.Dp) {
    val colors = HoopTheme.colors
    val shape = RoundedCornerShape(PublishSpec.thumbRadius)
    val bitmap = media?.let { rememberMediaBitmap(it.thumbnailUri ?: it.uri) }
    Box(Modifier.size(size).clip(shape).background(colors.questionContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap, stringResource(Res.string.queue_thumb), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(iconFor(type), null, tint = colors.onQuestionContainer, modifier = Modifier.size(PublishSpec.icon))
        }
    }
}

private fun iconFor(type: PostType): ImageVector = when (type) {
    PostType.Question -> HoopIcons.Chat
    PostType.Photo -> HoopIcons.Camera
    PostType.Video -> HoopIcons.Camera
}

@Composable
private fun typeLabel(type: PostType): String = stringResource(
    when (type) {
        PostType.Question -> Res.string.tag_question
        PostType.Photo -> Res.string.tag_photo
        PostType.Video -> Res.string.tag_video
    },
)

@Composable
private fun hoopName(state: HomeUiState, id: String): String =
    state.hoops.firstOrNull { it.id == id }?.let { it.name ?: stringResource(Res.string.hoop_home_name) }.orEmpty()

/** The sheet "In attesa di rete": one row per waiting post, each with an X to give it up. */
@Composable
internal fun QueueSheet(state: HomeUiState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = HoopTheme.colors
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    AnimatedVisibility(visible = state.queueOpen, modifier = modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(colors.scrim).clickable(role = Role.Button) { onIntent(HomeIntent.QueueClosed) }) {
            AnimatedVisibility(
                visible = state.queueOpen, modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it }, exit = slideOutVertically { it },
            ) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = PublishSpec.sheetRadius, topEnd = PublishSpec.sheetRadius))
                        .background(colors.surface).clickable(enabled = false) {}
                        .padding(start = PublishSpec.sheetPadding, end = PublishSpec.sheetPadding, top = PublishSpec.sheetPadding, bottom = navBottom + PublishSpec.sheetPadding),
                    verticalArrangement = Arrangement.spacedBy(PublishSpec.rowGap),
                ) {
                    Text(stringResource(Res.string.queue_sheet_title), style = HoopTheme.typography.titleMedium, color = colors.onSurface)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(PublishSpec.rowGap)) {
                        items(state.queued, key = { it.id }) { post -> QueueRow(post, hoopName(state, post.hoopId), onIntent) }
                    }
                    Text(stringResource(Res.string.queue_sheet_footer), style = HoopTheme.typography.caption, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QueueRow(post: QueuedPostUi, hoopName: String, onIntent: (HomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    val waiting = if (post.waitingMinutes <= 0) stringResource(Res.string.queue_waiting_now) else stringResource(Res.string.queue_waiting_minutes, post.waitingMinutes)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PublishSpec.rowGap)) {
        PostThumbnail(post.type, post.media, PublishSpec.rowThumb)
        Column(Modifier.weight(1f)) {
            Text(post.text, style = HoopTheme.typography.labelLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(Res.string.queue_row_subtitle, typeLabel(post.type), hoopName, waiting),
                style = HoopTheme.typography.caption, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        CloseButton(stringResource(Res.string.queue_cancel_post)) { onIntent(HomeIntent.QueuedPostCancelled(post.id)) }
    }
}

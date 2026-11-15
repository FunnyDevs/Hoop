package app.hoop.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hoop.designsystem.component.*
import app.hoop.designsystem.icon.HoopIcons
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.domain.identity.AppNotification
import app.hoop.domain.identity.NotificationKind
import app.hoop.domain.post.FeedPost
import app.hoop.presentation.home.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

/** Measures of the home, from the "Home" boards of the design (reference screen 390 x 844). */
private object HomeSpec {
    val headerGap = 10.dp
    val headerHeight = 44.dp
    val chipsGap = 14.dp
    val chipsHeight = 44.dp
    val chipsSpacing = 8.dp
    val chipsStart = 16.dp
    val chipsFade = 44.dp
    val infoGap = 14.dp
    val infoStart = 20.dp
    val cardsBottom = 28.dp
    val cardsStart = 16.dp
    val cardsGap = 14.dp
    val cardsEndPadding = 60.dp
    val dotsStart = 24.dp
    val dotsBottom = 10.dp
    val fabEnd = 16.dp
    val popoverWidth = 300.dp
    val popoverEnd = 12.dp
    val popoverGap = 2.dp
    val mapTopMargin = 24.dp
    val mapBottomMargin = 24.dp
    const val FOCUS_BLEND = 0.3
    const val TOAST_MILLIS = 1_900L
    const val STORY_FULL_OPEN = 0.999f
}

/** Entry point: connects the ViewModel to the screen. */
@Composable
fun HomeRoute(viewModel: HomeViewModel = koinViewModel(), config: HomeConfig = koinInject()) {
    val state by viewModel.state.collectAsState()
    HomeScreen(state, config, viewModel::onIntent)
}

@Composable
fun HomeScreen(state: HomeUiState, config: HomeConfig, onIntent: (HomeIntent) -> Unit) {
    val colors = HoopTheme.colors
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val headerTop = statusTop + HomeSpec.headerGap
    val chipsTop = headerTop + HomeSpec.headerHeight + HomeSpec.chipsGap
    val infoTop = chipsTop + HomeSpec.chipsHeight + HomeSpec.infoGap
    val cardsBottom = navBottom + HomeSpec.cardsBottom
    val cardBounds = remember { mutableStateMapOf<String, Rect>() }
    val active = state.activeHoop
    val scope = rememberCoroutineScope()
    val storyProgress = remember { Animatable(0f) }
    // True while the open card is being pulled by a finger rather than animating by itself.
    var dragOpening by remember { mutableStateOf(false) }
    var fingerDown by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.mapLand)) {
        // ---- Map ----
        if (active != null) {
            val areas = state.hoops.mapIndexed { index, hoop ->
                HoopMapArea(hoop.id, hoop.latitude ?: state.userLatitude, hoop.longitude ?: state.userLongitude, hoop.radiusMeters, colors.hoopAccents[index % colors.hoopAccents.size], hoop.id == active.id)
            }
            val pins = state.posts.mapIndexed { index, post ->
                HoopMapPin(post.id.value, post.location.latitude, post.location.longitude, post.type.toKind(), index == state.selectedIndex)
            }
            val centerLat = active.latitude ?: state.userLatitude
            val centerLon = active.longitude ?: state.userLongitude
            val post = state.selectedPost
            val focus = HoopMapFocus(
                latitude = if (post != null) centerLat + (post.location.latitude - centerLat) * HomeSpec.FOCUS_BLEND else centerLat,
                longitude = if (post != null) centerLon + (post.location.longitude - centerLon) * HomeSpec.FOCUS_BLEND else centerLon,
                radiusMeters = active.radiusMeters,
            )
            HoopHomeMap(
                styleUri = config.mapStyleUri, areas = areas, pins = pins,
                userLatitude = state.userLatitude, userLongitude = state.userLongitude,
                focus = focus, mapWidth = maxWidth,
                onPinClick = { id -> onIntent(HomeIntent.PostSelected(state.posts.indexOfFirst { it.id.value == id }.coerceAtLeast(0))) },
                questionGlyph = stringResource(Res.string.glyph_question),
                contentPadding = PaddingValues(
                    top = infoTop + HoopTheme.sizes.chipHeightLarge + HomeSpec.mapTopMargin,
                    bottom = cardsBottom + HoopPostCardDefaults.CarouselHeight + HomeSpec.mapBottomMargin,
                ),
            )
        }

        // ---- Header: name and notifications ----
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = headerTop).height(HomeSpec.headerHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HoopWordmark(stringResource(Res.string.brand_name))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                HoopMapIconButton(HoopIcons.Bell, stringResource(Res.string.action_notifications), { onIntent(HomeIntent.NotificationsToggled) }, dot = true)
            }
        }

        // ---- Hoop chips ----
        if (state.hoops.isNotEmpty()) {
            Box(Modifier.padding(top = chipsTop).fillMaxWidth().height(HomeSpec.chipsHeight + HoopTheme.spacing.sm)) {
                LazyRow(
                    contentPadding = PaddingValues(start = HomeSpec.chipsStart, end = HomeSpec.chipsStart),
                    horizontalArrangement = Arrangement.spacedBy(HomeSpec.chipsSpacing),
                ) {
                    itemsIndexed(state.hoops, key = { _, hoop -> hoop.id }) { index, hoop ->
                        HoopHoopChip(
                            name = hoop.name ?: stringResource(Res.string.hoop_home_name),
                            color = colors.hoopAccents[index % colors.hoopAccents.size], active = hoop.id == active?.id,
                            onClick = { onIntent(HomeIntent.HoopSelected(hoop.id)) },
                        )
                    }
                    item(key = "new-hoop") {
                        HoopNewHoopChip(stringResource(Res.string.action_new_hoop), { onIntent(HomeIntent.NewHoopRequested) })
                    }
                }
                Box(
                    Modifier.align(Alignment.CenterEnd).width(HomeSpec.chipsFade).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(colors.mapLand.copy(alpha = 0f), colors.mapLand.copy(alpha = 0.9f)))),
                )
            }
        }
        if (active != null) {
            HoopMapChip(
                stringResource(Res.string.hoop_info, active.name ?: stringResource(Res.string.hoop_home_name), formatDistance(active.radiusMeters), active.postCount),
                Modifier.padding(start = HomeSpec.infoStart, top = infoTop),
            )
        }

        // ---- Cards ----
        if (state.posts.isNotEmpty()) {
            val listState = rememberLazyListState()
            LaunchedEffect(state.activeHoopId) { listState.scrollToItem(0) }
            // A new post goes first: bring it into view.
            LaunchedEffect(state.posts.firstOrNull()?.id) { listState.animateScrollToItem(0) }
            // Only the user's own scrolling changes the selection; our scrolls to the selected card must not.
            var programmaticScroll by remember { mutableStateOf(false) }
            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                    if (!programmaticScroll && index != state.selectedIndex && listState.isScrollInProgress) onIntent(HomeIntent.PostSelected(index))
                }
            }
            LaunchedEffect(state.selectedIndex) {
                if (listState.firstVisibleItemIndex != state.selectedIndex && !listState.isScrollInProgress) {
                    programmaticScroll = true
                    try { listState.animateScrollToItem(state.selectedIndex) } finally { programmaticScroll = false }
                }
            }
            val openDragPx = with(LocalDensity.current) { StoryDragRange.toPx() }
            LazyRow(
                state = listState,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = cardsBottom).height(HoopPostCardDefaults.CarouselHeight).fillMaxWidth(),
                // Room after the last card so that any card, the last included, can sit in the first position.
                contentPadding = PaddingValues(start = HomeSpec.cardsStart, end = (maxWidth - HoopPostCardDefaults.CarouselWidth - HomeSpec.cardsStart).coerceAtLeast(HomeSpec.cardsEndPadding)),
                horizontalArrangement = Arrangement.spacedBy(HomeSpec.cardsGap),
                flingBehavior = rememberSnapFlingBehavior(listState),
            ) {
                itemsIndexed(state.posts, key = { _, post -> post.id.value }) { index, post ->
                    val shape = RoundedCornerShape(HoopPostCardDefaults.CornerRadius)
                    Box(
                        Modifier.width(HoopPostCardDefaults.CarouselWidth).height(HoopPostCardDefaults.CarouselHeight)
                            .alpha(if (state.openPostId != null) 0f else 1f)
                            .shadow(HoopTheme.elevation.card, shape)
                            .clip(shape)
                            .onGloballyPositioned { cardBounds[post.id.value] = it.boundsInRoot() }
                            .pointerInput(post.id.value) {
                                // Dragging a card upwards pulls it open: the story follows the finger.
                                var started = false
                                detectVerticalDragGestures(
                                    onDragStart = { started = false },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        if (!started && amount < 0f) {
                                            started = true
                                            dragOpening = true
                                            fingerDown = true
                                            onIntent(HomeIntent.PostOpened(post.id.value))
                                        }
                                        if (started) scope.launch { storyProgress.snapTo((storyProgress.value - amount / openDragPx).coerceIn(0f, 1f)) }
                                    },
                                    onDragEnd = {
                                        fingerDown = false
                                        if (started) scope.launch {
                                            if (storyProgress.value > STORY_OPEN_THRESHOLD) storyProgress.animateTo(1f, storySpring())
                                            else { storyProgress.animateTo(0f, storySpring()); dragOpening = false; onIntent(HomeIntent.PostClosed) }
                                        }
                                    },
                                    onDragCancel = {
                                        fingerDown = false
                                        if (started) scope.launch { storyProgress.animateTo(0f, storySpring()); dragOpening = false; onIntent(HomeIntent.PostClosed) }
                                    },
                                )
                            }
                            .clickable { onIntent(HomeIntent.PostOpened(post.id.value)) },
                    ) { PostCard(post, progress = 0f, flown = post.id.value in state.flownPostIds, bottomInset = navBottom, onFly = { onIntent(HomeIntent.FlyToggled(post.id.value)) }) }
                }
            }
            HoopPageDots(
                state.posts.size, state.selectedIndex,
                Modifier.align(Alignment.BottomStart).padding(start = HomeSpec.dotsStart, bottom = navBottom + HomeSpec.dotsBottom),
            )
        }

        // ---- New post ----
        // The scrim sits under the morphing button, which stays on top of it.
        AnimatedVisibility(state.askBarOpen || state.composer != null, enter = fadeIn(), exit = fadeOut()) {
            ComposeScrim(
                onClick = { onIntent(if (state.composer != null) HomeIntent.ComposerDismissed else HomeIntent.AskBarDismissed) },
                composerOpen = state.composer != null,
            )
        }
        ComposeMorph(
            stage = when {
                state.composer != null -> ComposeStage.SHEET
                state.askBarOpen -> ComposeStage.BAR
                else -> ComposeStage.BUTTON
            },
            screenWidth = maxWidth, composer = state.composer, hoop = active,
            onToggle = { onIntent(HomeIntent.AskBarToggled) }, onOpen = { onIntent(HomeIntent.ComposerOpened(it)) }, onIntent = onIntent,
        )

        // ---- Notifications ----
        AnimatedVisibility(
            visible = state.notificationsOpen,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = headerTop + HomeSpec.headerHeight + HomeSpec.popoverGap, end = HomeSpec.popoverEnd),
            enter = fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy), 0.6f, TransformOrigin(1f, 0f)),
            exit = fadeOut() + scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 0f)),
        ) {
            NotificationsPopover(state.notifications) { onIntent(HomeIntent.NotificationOpened(it)) }
        }

        // Once the card is completely open and released, it becomes the selected one (and the carousel scrolls to it).
        LaunchedEffect(state.openPostId) {
            val id = state.openPostId ?: return@LaunchedEffect
            snapshotFlow { storyProgress.value >= HomeSpec.STORY_FULL_OPEN && !fingerDown }.first { it }
            onIntent(HomeIntent.PostSelected(state.posts.indexOfFirst { it.id.value == id }.coerceAtLeast(0)))
        }

        // The keyboard going away folds the composer back into the ask bar.
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        var imeSeen by remember(state.composer != null) { mutableStateOf(false) }
        LaunchedEffect(state.composer != null, imeVisible) {
            if (state.composer == null) return@LaunchedEffect
            if (imeVisible) imeSeen = true else if (imeSeen) onIntent(HomeIntent.ComposerCollapsed)
        }

        // ---- Full-screen card ----
        state.openPost?.let { post ->
            val bounds = cardBounds[post.id.value] ?: with(LocalDensity.current) {
                centeredBounds(maxWidth.toPx(), maxHeight.toPx(), HoopPostCardDefaults.CarouselWidth.toPx(), HoopPostCardDefaults.CarouselHeight.toPx())
            }
            PostStory(
                posts = state.posts, openId = post.id.value, startBounds = bounds,
                boundsOf = { cardBounds[it] },
                cardsStartPx = with(LocalDensity.current) { HomeSpec.cardsStart.toPx() },
                replyText = state.replyText,
                onReplyTextChange = { onIntent(HomeIntent.ReplyTextChanged(it)) },
                onSend = { onIntent(HomeIntent.ReplySent) },
                onClosed = { dragOpening = false; onIntent(HomeIntent.PostClosed) },
                onPageChanged = { onIntent(HomeIntent.PostOpened(it)) },
                progress = storyProgress, autoExpand = !dragOpening, holding = fingerDown,
            ) { shown, progress, effects -> PostCard(shown, progress, shown.id.value in state.flownPostIds, navBottom, { onIntent(HomeIntent.FlyToggled(shown.id.value)) }, effects) }
        }

        // ---- Toast ----
        val toast = state.toast
        AnimatedVisibility(
            visible = toast != null, modifier = Modifier.align(Alignment.TopCenter).padding(top = infoTop + HoopTheme.sizes.chipHeightLarge + HoopTheme.spacing.sm),
            enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it },
        ) {
            val name = state.hoops.firstOrNull { it.id == toast?.hoopId }?.let { it.name ?: stringResource(Res.string.hoop_home_name) }.orEmpty()
            HoopToast(stringResource(Res.string.toast_published, name))
        }
        LaunchedEffect(toast) { if (toast != null) { delay(HomeSpec.TOAST_MILLIS); onIntent(HomeIntent.ToastShown) } }
    }
}

/** Where a card sits when its place in the carousel is unknown (for example when opened from a notification). */
private fun centeredBounds(widthPx: Float, heightPx: Float, cardWidthPx: Float, cardHeightPx: Float): Rect =
    Rect((widthPx - cardWidthPx) / 2f, (heightPx - cardHeightPx) / 2f, (widthPx + cardWidthPx) / 2f, (heightPx + cardHeightPx) / 2f)

@Composable
private fun PostCard(
    post: FeedPost, progress: Float, flown: Boolean, bottomInset: Dp, onFly: () -> Unit,
    effects: PostCardEffects = PostCardEffects.None,
) {
    val colors = HoopTheme.colors
    val flies = post.fliesCount + if (flown) 1 else 0
    HoopPostCard(
        kind = post.type.toKind(),
        title = post.text,
        fliesCount = formatCount(flies),
        repliesCount = formatCount(post.replies.size),
        authorInitials = initialsOf(post.author.name),
        authorHandle = stringResource(Res.string.author_handle, post.author.name.lowercase()),
        ageLabel = formatAge(post.ageMinutes),
        streetLabel = stringResource(Res.string.street_distance, post.street, formatDistance(post.distanceMeters)),
        fliesLabel = pluralStringResource(Res.plurals.flies_label, flies),
        repliesLabel = repliesPillLabel(post),
        flown = flown,
        onFlyClick = onFly,
        onRepliesClick = effects.onComments,
        questionColor = questionColorOf(post),
        media = mediaPainter(post.mediaKey),
        progress = progress,
        bottomInset = bottomInset,
        sheetProgress = effects.sheetProgress,
        immersion = effects.immersion,
        immersionSign = effects.immersionSign,
        onRepliesBounds = effects.onCommentsBounds,
    )
}

@Composable
private fun NotificationsPopover(items: List<AppNotification>, onClick: (String) -> Unit) {
    val colors = HoopTheme.colors
    Column(
        Modifier.width(HomeSpec.popoverWidth).shadow(HoopTheme.elevation.card, RoundedCornerShape(24.dp))
            .background(colors.surface, RoundedCornerShape(24.dp)).padding(HoopTheme.spacing.sm),
    ) {
        items.forEach { n ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick(n.id) }.padding(HoopTheme.spacing.sm + 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HoopTheme.spacing.sm + 2.dp),
            ) {
                Box(Modifier.size(34.dp).background(colors.avatarSoft, HoopTheme.shapes.circle), contentAlignment = Alignment.Center) {
                    Text(initialsOf(n.actorName), style = HoopTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = colors.onSurface)
                }
                Text(
                    when (n.kind) {
                        NotificationKind.RepliesReceived -> stringResource(Res.string.notification_replies, n.actorName, n.count)
                        NotificationKind.VideoPublished -> stringResource(Res.string.notification_video, n.actorName, formatDistance(n.distanceMeters))
                        NotificationKind.QuestionPublished -> stringResource(Res.string.notification_question, n.actorName, formatDistance(n.distanceMeters))
                    },
                    style = HoopTheme.typography.bodyMedium.copy(fontSize = HoopTheme.typography.labelMedium.fontSize * 1.08f), color = colors.onSurface,
                )
            }
        }
    }
}

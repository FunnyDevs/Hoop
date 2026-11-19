package app.hoop.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.common.getOrNull
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostId
import app.hoop.domain.post.PostType
import app.hoop.presentation.home.camera.isVideo
import app.hoop.domain.usecase.AddReplyUseCase
import app.hoop.domain.usecase.GetHoopFeedUseCase
import app.hoop.domain.usecase.GetNotificationsUseCase
import app.hoop.domain.usecase.GetRepliesUseCase
import app.hoop.domain.usecase.SetFlyUseCase
import app.hoop.domain.usecase.SetReplyFlyUseCase
import app.hoop.domain.post.FlyState
import app.hoop.domain.usecase.GetUserHoopsUseCase
import app.hoop.domain.usecase.CancelPendingPostUseCase
import app.hoop.domain.usecase.GetPendingPostsUseCase
import app.hoop.domain.usecase.PublishOutcome
import app.hoop.domain.usecase.QueueCause
import app.hoop.domain.usecase.PublishPostUseCase
import app.hoop.domain.usecase.SendPendingPostsUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import app.hoop.domain.usecase.RequestLocationUseCase
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class HomeViewModel(
    private val config: HomeConfig,
    private val requestLocation: RequestLocationUseCase,
    private val getUserHoops: GetUserHoopsUseCase,
    private val getFeed: GetHoopFeedUseCase,
    private val getReplies: GetRepliesUseCase,
    private val addReply: AddReplyUseCase,
    private val setFlyOnServer: SetFlyUseCase,
    private val setReplyFlyOnServer: SetReplyFlyUseCase,
    private val getNotifications: GetNotificationsUseCase,
    private val publishPost: PublishPostUseCase,
    private val sendPendingPosts: SendPendingPostsUseCase,
    private val getPendingPosts: GetPendingPostsUseCase,
    private val cancelPendingPost: CancelPendingPostUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState(userLatitude = config.fallbackLatitude, userLongitude = config.fallbackLongitude))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var hoops: UserHoops? = null
    private var feeds: Map<String, List<FeedPost>> = emptyMap()
    /** Where the device was when the position was last read; `null` if it was never known. */
    private var knownLocation: GeoPoint? = null
    /** The camera the user was last using; a retake reopens on it. */
    private var frontLens = false

    init {
        viewModelScope.launch { bootstrap() }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.HoopSelected -> selectHoop(intent.id)
            // The "add a Hoop" flow (ghost circle on the map) is not built yet.
            HomeIntent.NewHoopRequested -> Unit
            is HomeIntent.PostSelected -> _state.update { it.copy(selectedIndex = intent.index.coerceIn(0, (it.posts.size - 1).coerceAtLeast(0))) }
            // The carousel keeps its position until the card is completely open (see PostSelected).
            is HomeIntent.PostOpened -> {
                _state.update { it.copy(openPostId = intent.id, replyText = "") }
                loadReplies(intent.id)
            }
            is HomeIntent.FlyToggled -> toggleFly(intent.id)
            is HomeIntent.ReplyFlyToggled -> toggleReplyFly(intent.postId, intent.replyId)
            HomeIntent.PostClosed -> _state.update { it.copy(openPostId = null, replyText = "") }
            is HomeIntent.ReplyTextChanged -> _state.update { it.copy(replyText = intent.value) }
            HomeIntent.ReplySent -> sendReply()
            HomeIntent.AskBarToggled -> _state.update { it.copy(askBarOpen = !it.askBarOpen, notificationsOpen = false) }
            HomeIntent.AskBarDismissed -> _state.update { it.copy(askBarOpen = false) }
            is HomeIntent.ComposerOpened -> _state.update { it.copy(composer = ComposerUi(intent.type), askBarOpen = false) }
            is HomeIntent.ComposerTextChanged -> _state.update { s -> s.copy(composer = s.composer?.let { ComposerUi(it.type, intent.value, it.media, false) }) }
            HomeIntent.CameraRequested -> _state.update { it.copy(capture = CaptureUi(frontLens = frontLens)) }
            HomeIntent.CameraClosed -> _state.update { it.copy(capture = null) }
            // Nothing to redraw: the lens is only read the next time the camera opens.
            is HomeIntent.CameraLensChanged -> frontLens = intent.front
            // The position is the last one the device gave us; it is never asked for here.
            is HomeIntent.MediaCaptured -> _state.update { it.copy(capture = CaptureUi(intent.media.copy(location = knownLocation), frontLens)) }
            HomeIntent.CaptureDiscarded -> _state.update { it.copy(capture = CaptureUi(frontLens = frontLens)) }
            HomeIntent.CaptureConfirmed -> confirmCapture()
            HomeIntent.ComposerPublished -> publish()
            HomeIntent.ComposerDismissed -> _state.update { it.copy(composer = null) }
            HomeIntent.ComposerCollapsed -> _state.update { it.copy(composer = null, askBarOpen = true) }
            HomeIntent.NotificationsToggled -> toggleNotifications()
            is HomeIntent.NotificationOpened -> openNotification(intent.id)
            HomeIntent.ToastShown -> _state.update { it.copy(toast = null) }
            HomeIntent.PublishCancelled -> cancelPublish()
            HomeIntent.PublishRetried -> retryPublish()
            HomeIntent.PublishFailureDismissed -> dismissPublishFailure()
            HomeIntent.QueueOpened -> _state.update { it.copy(queueOpen = it.queued.isNotEmpty()) }
            HomeIntent.QueueClosed -> _state.update { it.copy(queueOpen = false) }
            is HomeIntent.QueuedPostCancelled -> cancelQueuedPost(intent.id)
            // Handled by the screen that hosts the home: it leaves for the sign-in.
            HomeIntent.SignInRequested -> Unit
        }
    }

    /** Sends the posts that waited for the connection and, if any went, reloads the feed of their Hoop. */
    private suspend fun sendWaitingPosts() = keepGoingOnFailure {
        val result = sendPendingPosts()
        if (result.sent > 0) refreshFeeds()
        // If the session is the reason it stopped, the card asks to sign in again.
        _state.update { it.copy(needsSignIn = result.stoppedBy == DomainError.Unauthorized) }
        refreshQueue()
    }

    /** The queue is a convenience: if the local database fails, the home still works. */
    private suspend fun keepGoingOnFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.withTag("Hoop").w(e) { "queue of waiting posts failed" }
        }
    }

    private suspend fun refreshFeeds() {
        val aggregate = hoops ?: return
        feeds = aggregate.all.associate { hoop -> hoop.id.value to (getFeed(hoop, position()).getOrNull() ?: feeds[hoop.id.value].orEmpty()) }
        publishState()
    }

    private suspend fun bootstrap() {
        requestLocation().getOrNull()?.let { point ->
            knownLocation = point
            _state.update { it.copy(userLatitude = point.latitude, userLongitude = point.longitude) }
        }
        val aggregate = getUserHoops().getOrNull() ?: return _state.update { it.copy(isLoading = false) }
        hoops = aggregate
        feeds = aggregate.all.associate { hoop -> hoop.id.value to (getFeed(hoop, position()).getOrNull() ?: emptyList()) }
        _state.update { it.copy(isLoading = false, activeHoopId = aggregate.home.id.value) }
        publishState(selectedIndex = 0)
        // Posts kept on the device while there was no connection go out as soon as the home opens.
        sendWaitingPosts()
    }

    private fun position(): GeoPoint =
        (GeoPoint.create(_state.value.userLatitude, _state.value.userLongitude) as? DomainResult.Success)?.value
            ?: error("The position was validated before")

    /** Rebuilds the derived parts of the state from the aggregate and the loaded feeds. */
    private fun publishState(selectedIndex: Int = _state.value.selectedIndex) {
        val aggregate = hoops ?: return
        _state.update { s ->
            val active = s.activeHoopId ?: aggregate.home.id.value
            val posts = feeds[active].orEmpty()
            s.copy(
                hoops = aggregate.all.map { it.toUi(feeds[it.id.value]?.size ?: 0) },
                activeHoopId = active,
                posts = posts,
                selectedIndex = selectedIndex.coerceIn(0, (posts.size - 1).coerceAtLeast(0)),
            )
        }
    }

    private fun selectHoop(id: String) {
        _state.update { it.copy(activeHoopId = id, selectedIndex = 0, openPostId = null) }
        publishState(selectedIndex = 0)
    }

    /** The feed only counts the replies of a post: they are read when the post opens. */
    private fun loadReplies(postId: String) {
        viewModelScope.launch {
            getReplies(PostId(postId)).getOrNull()?.let { loaded ->
                feeds = feeds.mapValues { (_, list) -> list.map { if (it.id.value == postId) it.withReplies(loaded) else it } }
                publishState()
            }
        }
    }

    /** Posts and replies whose "Ci volo" is being sent: a second tap on the same one waits for the answer. */
    private val flyingNow = mutableSetOf<String>()

    /**
     * "Ci volo" shows at once and is then confirmed by the server (which also gives the true count);
     * if the server cannot be reached the old state comes back.
     */
    private fun toggleFly(postId: String) {
        val before = findPost(postId) ?: return
        if (!flyingNow.add(postId)) return
        val guess = FlyState(!before.flownByMe, (before.fliesCount + if (before.flownByMe) -1 else 1).coerceAtLeast(0))
        updatePost(postId) { it.withFly(guess) }
        viewModelScope.launch {
            val answer = setFlyOnServer(PostId(postId), guess.flown).getOrNull()
            updatePost(postId) { it.withFly(answer ?: FlyState(before.flownByMe, before.fliesCount)) }
            flyingNow.remove(postId)
        }
    }

    private fun toggleReplyFly(postId: String, replyId: String) {
        val before = findPost(postId)?.replies?.firstOrNull { it.id == replyId } ?: return
        if (!flyingNow.add(replyId)) return
        val guess = FlyState(!before.flownByMe, (before.fliesCount + if (before.flownByMe) -1 else 1).coerceAtLeast(0))
        updatePost(postId) { it.withReplyFly(replyId, guess) }
        viewModelScope.launch {
            val answer = setReplyFlyOnServer(replyId, guess.flown).getOrNull()
            updatePost(postId) { it.withReplyFly(replyId, answer ?: FlyState(before.flownByMe, before.fliesCount)) }
            flyingNow.remove(replyId)
        }
    }

    private fun findPost(postId: String): FeedPost? = feeds.values.firstNotNullOfOrNull { list -> list.firstOrNull { it.id.value == postId } }

    private fun updatePost(postId: String, change: (FeedPost) -> FeedPost) {
        feeds = feeds.mapValues { (_, list) -> list.map { if (it.id.value == postId) change(it) else it } }
        publishState()
    }

    private fun sendReply() {
        val s = _state.value
        val post = s.openPost ?: return
        if (s.replyText.isBlank()) return
        viewModelScope.launch {
            addReply(post.id, s.replyText).getOrNull()?.let { reply ->
                feeds = feeds.mapValues { (_, list) -> list.map { if (it.id == post.id) it.withReply(reply) else it } }
                _state.update { it.copy(replyText = "") }
                publishState()
            }
        }
    }

    /** The post being published: a retry of the very same content keeps its identifier, so the server never duplicates it. */
    private class PublishAttempt(val content: String, val clientId: String)

    /** What the user asked to publish, kept so a failed post can be retried with the same identifier. */
    private class PublishRequest(
        val hoop: Hoop,
        val type: PostType,
        val text: String,
        val media: MediaRef?,
        val clientId: String,
    )

    private var attempt: PublishAttempt? = null
    private var publishJob: Job? = null
    private var lastRequest: PublishRequest? = null

    @OptIn(ExperimentalUuidApi::class)
    private fun publish() {
        // A second tap while the first request is running must not start another one.
        if (publishJob?.isActive == true) return
        val s = _state.value
        val composer = s.composer ?: return
        val hoop = hoops?.find(hoopId(s.activeHoopId)) ?: return
        val media = if (composer.type == PostType.Question) null else composer.media
        val content = listOf(hoop.id.value, composer.type, composer.text, media?.uri).joinToString(CONTENT_SEPARATOR)
        val clientId = attempt?.takeIf { it.content == content }?.clientId
            ?: Uuid.random().toString().also { attempt = PublishAttempt(content, it) }
        // The composer closes at once: the progress shows on the card at the top and the user keeps browsing.
        _state.update { it.copy(composer = null) }
        send(PublishRequest(hoop, composer.type, composer.text, media, clientId))
    }

    private fun send(request: PublishRequest) {
        lastRequest = request
        _state.update { it.copy(publishing = PublishingUi(request.type, request.hoop.id.value, request.media)) }
        publishJob = viewModelScope.launch {
            val here = freshPosition()
            val result = publishPost(request.hoop, request.type, request.text, request.media, here, request.clientId) { progress ->
                _state.update { st -> st.copy(publishing = st.publishing?.takeIf { it.failure == null }?.withProgress(progress)) }
            }
            when (result) {
                is DomainResult.Success -> when (val outcome = result.value) {
                    is PublishOutcome.Published -> {
                        attempt = null
                        feeds = feeds + (request.hoop.id.value to (getFeed(request.hoop, position()).getOrNull() ?: feeds[request.hoop.id.value].orEmpty()))
                        _state.update { it.copy(publishing = null, toast = ToastUi(ToastKind.Published, request.hoop.id.value)) }
                        publishState(selectedIndex = 0)
                        // The connection works: waiting posts can go too.
                        sendWaitingPosts()
                    }
                    // Kept on the device: it goes out by itself, so the card of the queue takes over.
                    is PublishOutcome.Queued -> {
                        attempt = null
                        _state.update { it.copy(publishing = null, needsSignIn = outcome.cause == QueueCause.SessionExpired) }
                        refreshQueue()
                    }
                }
                is DomainResult.Failure -> showFailure(request, result.error)
            }
        }
    }

    private fun showFailure(request: PublishRequest, error: DomainError) {
        val failure = when {
            error == DomainError.Validation(Violation.PositionOutsideHoop) -> PublishFailure.TooFar
            error == DomainError.TooManyRequests -> PublishFailure.TooManyRequests
            // The text or the shot was not acceptable: back to the composer, which says what is missing.
            error is DomainError.Validation && error.violation != Violation.HoopNotFound && error.violation != Violation.PostTypeNotAllowedInHoop -> {
                attempt = null
                _state.update { it.copy(publishing = null, composer = ComposerUi(request.type, request.text, request.media, showError = true)) }
                return
            }
            else -> PublishFailure.Generic
        }
        _state.update { it.copy(publishing = it.publishing?.copy(failure = failure)) }
    }

    /** The X of the card while uploading: the post is dropped and nothing is kept. */
    private fun cancelPublish() {
        publishJob?.cancel()
        attempt = null
        _state.update { it.copy(publishing = null) }
    }

    private fun retryPublish() {
        val request = lastRequest ?: return
        if (publishJob?.isActive == true) return
        send(request)
    }

    private fun dismissPublishFailure() {
        attempt = null
        _state.update { it.copy(publishing = null) }
    }

    /** Reads the waiting posts again for the card and the sheet of the queue. */
    private suspend fun refreshQueue() = keepGoingOnFailure {
        val waiting = getPendingPosts().map { item ->
            QueuedPostUi(
                id = item.draft.clientId,
                type = item.draft.type,
                text = item.draft.text.value,
                hoopId = item.draft.hoopId.value,
                media = item.draft.media,
                waitingMinutes = item.waitingMinutes,
            )
        }
        _state.update { it.copy(queued = waiting, queueOpen = it.queueOpen && waiting.isNotEmpty(), needsSignIn = it.needsSignIn && waiting.isNotEmpty()) }
    }

    private fun cancelQueuedPost(id: String) {
        viewModelScope.launch {
            cancelPendingPost(id)
            refreshQueue()
        }
    }

    /**
     * The position to publish from. The one read when the home opened may be old, and the post belongs to
     * where the user is now, so it is read again (the device answers at once when its last fix is fresh).
     * If the device cannot tell in time, the last known position is used.
     */
    private suspend fun freshPosition(): GeoPoint {
        val fresh = withTimeoutOrNull(PUBLISH_LOCATION_TIMEOUT_MILLIS) { requestLocation().getOrNull() }
        if (fresh != null) knownLocation = fresh
        return fresh ?: position()
    }

    /** Keeps the shot: the composer opens (or, after a retake, gets the new shot) with the caption already typed, if any. */
    private fun confirmCapture() {
        _state.update { s ->
            val media = s.capture?.media ?: return@update s
            val type = if (media.isVideo) PostType.Video else PostType.Photo
            s.copy(composer = ComposerUi(type, s.composer?.text.orEmpty(), media), capture = null, askBarOpen = false)
        }
    }

    private fun toggleNotifications() {
        val open = !_state.value.notificationsOpen
        _state.update { it.copy(notificationsOpen = open, askBarOpen = false) }
        if (open) viewModelScope.launch {
            getNotifications().getOrNull()?.let { list -> _state.update { it.copy(notifications = list) } }
        }
    }

    private fun openNotification(id: String) {
        val notification = _state.value.notifications.firstOrNull { it.id == id } ?: return
        // Posts do not say which Hoop they belong to: it is the feed that holds the post.
        val hoopId = feeds.entries.firstOrNull { (_, list) -> list.any { it.id == notification.postId } }?.key
        if (hoopId == null) return _state.update { it.copy(notificationsOpen = false) }
        _state.update { it.copy(activeHoopId = hoopId, notificationsOpen = false) }
        publishState(selectedIndex = 0)
        _state.update { s ->
            s.copy(openPostId = notification.postId.value, selectedIndex = s.posts.indexOfFirst { it.id == notification.postId }.coerceAtLeast(0))
        }
        loadReplies(notification.postId.value)
    }

    private fun hoopId(id: String?) = app.hoop.domain.hoop.HoopId(id.orEmpty())

    private fun Hoop.toUi(postCount: Int): HoopUi = when (this) {
        is Hoop.Home -> HoopUi(id.value, true, null, radius.meters, null, null, postCount, acceptedPostTypes)
        is Hoop.Extra -> HoopUi(id.value, false, name, radius.meters, center.latitude, center.longitude, postCount, acceptedPostTypes)
    }
}

private const val PUBLISH_LOCATION_TIMEOUT_MILLIS = 5_000L
private const val CONTENT_SEPARATOR = "\u0000"

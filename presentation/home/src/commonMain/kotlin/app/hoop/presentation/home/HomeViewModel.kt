package app.hoop.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.common.getOrNull
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.MediaRef
import app.hoop.domain.post.PostType
import app.hoop.domain.usecase.AddReplyUseCase
import app.hoop.domain.usecase.GetHoopFeedUseCase
import app.hoop.domain.usecase.GetNotificationsUseCase
import app.hoop.domain.usecase.GetUserHoopsUseCase
import app.hoop.domain.usecase.PublishPostUseCase
import app.hoop.domain.usecase.RequestLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val config: HomeConfig,
    private val requestLocation: RequestLocationUseCase,
    private val getUserHoops: GetUserHoopsUseCase,
    private val getFeed: GetHoopFeedUseCase,
    private val addReply: AddReplyUseCase,
    private val getNotifications: GetNotificationsUseCase,
    private val publishPost: PublishPostUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState(userLatitude = config.fallbackLatitude, userLongitude = config.fallbackLongitude))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var hoops: UserHoops? = null
    private var feeds: Map<String, List<FeedPost>> = emptyMap()

    init {
        viewModelScope.launch { bootstrap() }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.HoopSelected -> selectHoop(intent.id)
            // The "add a Hoop" flow (ghost circle on the map) is not built yet.
            HomeIntent.NewHoopRequested -> Unit
            is HomeIntent.PostSelected -> _state.update { it.copy(selectedIndex = intent.index.coerceIn(0, (it.posts.size - 1).coerceAtLeast(0))) }
            is HomeIntent.PostOpened -> _state.update { s ->
                s.copy(openPostId = intent.id, replyText = "", selectedIndex = s.posts.indexOfFirst { it.id.value == intent.id }.coerceAtLeast(0))
            }
            HomeIntent.PostClosed -> _state.update { it.copy(openPostId = null, replyText = "") }
            is HomeIntent.ReplyTextChanged -> _state.update { it.copy(replyText = intent.value) }
            HomeIntent.ReplySent -> sendReply()
            HomeIntent.AskBarToggled -> _state.update { it.copy(askBarOpen = !it.askBarOpen, notificationsOpen = false) }
            HomeIntent.AskBarDismissed -> _state.update { it.copy(askBarOpen = false) }
            is HomeIntent.ComposerOpened -> _state.update { it.copy(composer = ComposerUi(intent.type), askBarOpen = false) }
            is HomeIntent.ComposerTypeChanged -> _state.update { s -> s.copy(composer = s.composer?.let { ComposerUi(intent.type, it.text, it.mediaIndex, it.showError) }) }
            is HomeIntent.ComposerTextChanged -> _state.update { s -> s.copy(composer = s.composer?.let { ComposerUi(it.type, intent.value, it.mediaIndex, false) }) }
            is HomeIntent.ComposerMediaPicked -> _state.update { s -> s.copy(composer = s.composer?.let { ComposerUi(it.type, it.text, intent.index, it.showError) }) }
            HomeIntent.ComposerPublished -> publish()
            HomeIntent.ComposerDismissed -> _state.update { it.copy(composer = null) }
            HomeIntent.NotificationsToggled -> toggleNotifications()
            is HomeIntent.NotificationOpened -> openNotification(intent.id)
            HomeIntent.ToastShown -> _state.update { it.copy(toast = null) }
        }
    }

    private suspend fun bootstrap() {
        requestLocation().getOrNull()?.let { point -> _state.update { it.copy(userLatitude = point.latitude, userLongitude = point.longitude) } }
        val aggregate = getUserHoops().getOrNull() ?: return _state.update { it.copy(isLoading = false) }
        hoops = aggregate
        feeds = aggregate.all.associate { hoop -> hoop.id.value to (getFeed(hoop, position()).getOrNull() ?: emptyList()) }
        _state.update { it.copy(isLoading = false, activeHoopId = aggregate.home.id.value) }
        publishState(selectedIndex = 0)
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

    private fun publish() {
        val s = _state.value
        val composer = s.composer ?: return
        val hoop = hoops?.find(hoopId(s.activeHoopId)) ?: return
        val media = if (composer.type == PostType.Question) null else MediaRef(SampleMedia.keyAt(composer.mediaIndex), SampleMedia.MIME_TYPE)
        viewModelScope.launch {
            when (val result = publishPost(hoop, composer.type, composer.text, media)) {
                is DomainResult.Success -> {
                    feeds = feeds + (hoop.id.value to (getFeed(hoop, position()).getOrNull() ?: feeds[hoop.id.value].orEmpty()))
                    _state.update { it.copy(composer = null, toast = ToastUi(ToastKind.Published, hoop.id.value)) }
                    publishState(selectedIndex = 0)
                }
                is DomainResult.Failure -> _state.update { st ->
                    st.copy(composer = st.composer?.let { ComposerUi(it.type, it.text, it.mediaIndex, showError = true) })
                }
            }
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
        _state.update { it.copy(activeHoopId = notification.hoopId.value, notificationsOpen = false) }
        publishState(selectedIndex = 0)
        _state.update { s ->
            s.copy(openPostId = notification.postId.value, selectedIndex = s.posts.indexOfFirst { it.id == notification.postId }.coerceAtLeast(0))
        }
    }

    private fun hoopId(id: String?) = app.hoop.domain.hoop.HoopId(id.orEmpty())

    private fun Hoop.toUi(postCount: Int): HoopUi = when (this) {
        is Hoop.Home -> HoopUi(id.value, true, null, radius.meters, null, null, postCount, acceptedPostTypes)
        is Hoop.Extra -> HoopUi(id.value, false, name, radius.meters, center.latitude, center.longitude, postCount, acceptedPostTypes)
    }
}

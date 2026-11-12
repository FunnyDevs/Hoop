package app.hoop.presentation.home

import app.hoop.domain.identity.AppNotification
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.PostType

/** A Hoop as shown in the bar and drawn on the map. */
class HoopUi(
    val id: String,
    val isHome: Boolean,
    /** `null` for the initial Hoop, whose name is a localized string. */
    val name: String?,
    val radiusMeters: Int,
    /** `null` for the initial Hoop: it follows the user's position. */
    val latitude: Double?,
    val longitude: Double?,
    val postCount: Int,
    val acceptedTypes: Set<PostType>,
)

enum class ToastKind { Published }

class ToastUi(val kind: ToastKind, val hoopId: String)

class ComposerUi(val type: PostType, val text: String = "", val mediaIndex: Int = 0, val showError: Boolean = false)

data class HomeUiState(
    val isLoading: Boolean = true,
    val userLatitude: Double,
    val userLongitude: Double,
    val hoops: List<HoopUi> = emptyList(),
    val activeHoopId: String? = null,
    val posts: List<FeedPost> = emptyList(),
    val selectedIndex: Int = 0,
    val openPostId: String? = null,
    val replyText: String = "",
    /** Posts the user said "Ci volo" to (local until the server supports it). */
    val flownPostIds: Set<String> = emptySet(),
    val askBarOpen: Boolean = false,
    val composer: ComposerUi? = null,
    val notificationsOpen: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val toast: ToastUi? = null,
) {
    val activeHoop: HoopUi? get() = hoops.firstOrNull { it.id == activeHoopId }
    val selectedPost: FeedPost? get() = posts.getOrNull(selectedIndex)
    val openPost: FeedPost? get() = posts.firstOrNull { it.id.value == openPostId }
}

sealed interface HomeIntent {
    data class HoopSelected(val id: String) : HomeIntent
    data object NewHoopRequested : HomeIntent
    /** The keyboard was closed while writing: the composer folds back into the ask bar. */
    data object ComposerCollapsed : HomeIntent
    data class PostSelected(val index: Int) : HomeIntent
    data class PostOpened(val id: String) : HomeIntent
    data object PostClosed : HomeIntent
    data class FlyToggled(val id: String) : HomeIntent
    data class ReplyTextChanged(val value: String) : HomeIntent
    data object ReplySent : HomeIntent
    data object AskBarToggled : HomeIntent
    data object AskBarDismissed : HomeIntent
    data class ComposerOpened(val type: PostType) : HomeIntent
    data class ComposerTypeChanged(val type: PostType) : HomeIntent
    data class ComposerTextChanged(val value: String) : HomeIntent
    data class ComposerMediaPicked(val index: Int) : HomeIntent
    data object ComposerPublished : HomeIntent
    data object ComposerDismissed : HomeIntent
    data object NotificationsToggled : HomeIntent
    data class NotificationOpened(val id: String) : HomeIntent
    data object ToastShown : HomeIntent
}

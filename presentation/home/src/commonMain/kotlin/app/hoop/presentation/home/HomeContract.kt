package app.hoop.presentation.home

import app.hoop.domain.identity.AppNotification
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.MediaRef
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

/** The sheet that writes a post. A photo or video post carries the one [media] taken live with the camera. */
class ComposerUi(val type: PostType, val text: String = "", val media: MediaRef? = null, val showError: Boolean = false)

/** Why a post could not be published and cannot just wait in the queue. */
enum class PublishFailure { TooFar, TooManyRequests, Generic }

/**
 * The post being sent, shown as a card at the top while it uploads ([progress], 0 to 1) or, if it failed, with the
 * reason ([failure]). [media] gives the thumbnail; a question has none.
 */
data class PublishingUi(
    val type: PostType,
    val hoopId: String,
    val media: MediaRef?,
    val progress: Float = 0f,
    val failure: PublishFailure? = null,
) {
    fun withProgress(value: Float) = copy(progress = value.coerceIn(0f, 1f))
}

/** A post kept on the device until the connection is back. */
class QueuedPostUi(
    val id: String,
    val type: PostType,
    val text: String,
    val hoopId: String,
    val media: MediaRef?,
    val waitingMinutes: Int,
)

/** The full-screen camera flow: live camera while [media] is `null`, the confirmation of the shot once it exists. */
class CaptureUi(val media: MediaRef? = null, val frontLens: Boolean = false)

/**
 * The server cannot be reached: the home shows the last data it saved, dimmed, with a notice. [lastUpdatedMillis] is when
 * the data was last loaded successfully (`null` if unknown); [retrying] is true while a new attempt is under way.
 */
data class OfflineUi(val lastUpdatedMillis: Long? = null, val retrying: Boolean = false)

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
    val askBarOpen: Boolean = false,
    val composer: ComposerUi? = null,
    val capture: CaptureUi? = null,
    val notificationsOpen: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val toast: ToastUi? = null,
    val publishing: PublishingUi? = null,
    /** Posts waiting for the connection, oldest first. */
    val queued: List<QueuedPostUi> = emptyList(),
    /** The sheet listing [queued] is open. */
    val queueOpen: Boolean = false,
    /** The waiting posts cannot go out until the user signs in again. */
    val needsSignIn: Boolean = false,
    /** Not `null` while the data on screen is the saved copy because the server does not answer. */
    val offline: OfflineUi? = null,
) {
    val activeHoop: HoopUi? get() = hoops.firstOrNull { it.id == activeHoopId }
    val selectedPost: FeedPost? get() = posts.getOrNull(selectedIndex)
    val openPost: FeedPost? get() = posts.firstOrNull { it.id.value == openPostId }
}

sealed interface HomeIntent {
    data class HoopSelected(val id: String) : HomeIntent
    data object NewHoopRequested : HomeIntent
    /** "Riprova" of the offline notice: load everything again. */
    data object RetryRequested : HomeIntent
    /** The keyboard was closed while writing: the composer folds back into the ask bar. */
    data object ComposerCollapsed : HomeIntent
    data class PostSelected(val index: Int) : HomeIntent
    data class PostOpened(val id: String) : HomeIntent
    data object PostClosed : HomeIntent
    data class FlyToggled(val id: String) : HomeIntent
    data class ReplyFlyToggled(val postId: String, val replyId: String) : HomeIntent
    data class ReplyTextChanged(val value: String) : HomeIntent
    data object ReplySent : HomeIntent
    data object AskBarToggled : HomeIntent
    data object AskBarDismissed : HomeIntent
    data class ComposerOpened(val type: PostType) : HomeIntent
    data class ComposerTextChanged(val value: String) : HomeIntent
    /** The camera button of the ask bar, or the retake button of the composer. */
    data object CameraRequested : HomeIntent
    /** The X of the live camera: leave without keeping anything. */
    data object CameraClosed : HomeIntent
    /** The user switched between the back and the front camera: the next time the camera opens, it opens on that one. */
    data class CameraLensChanged(val front: Boolean) : HomeIntent
    /** The camera took a photo or finished a video (the position is added by the ViewModel). */
    data class MediaCaptured(val media: MediaRef) : HomeIntent
    /** The X of the confirmation: throw the shot away and go back to the live camera. */
    data object CaptureDiscarded : HomeIntent
    /** The tick of the confirmation: keep the shot and write its caption. */
    data object CaptureConfirmed : HomeIntent
    data object ComposerPublished : HomeIntent
    data object ComposerDismissed : HomeIntent
    data object NotificationsToggled : HomeIntent
    data class NotificationOpened(val id: String) : HomeIntent
    data object ToastShown : HomeIntent
    /** The X of the card while a post uploads. */
    data object PublishCancelled : HomeIntent
    data object PublishRetried : HomeIntent
    /** The button of the card of a post that could not be published and has nothing to retry. */
    data object PublishFailureDismissed : HomeIntent
    data object QueueOpened : HomeIntent
    data object QueueClosed : HomeIntent
    data class QueuedPostCancelled(val id: String) : HomeIntent
    /** The session expired: the home hands over to the sign-in. */
    data object SignInRequested : HomeIntent
}

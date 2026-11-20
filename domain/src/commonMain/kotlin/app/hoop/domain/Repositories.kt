package app.hoop.domain

import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.identity.AppNotification
import app.hoop.domain.post.FeedPost
import app.hoop.domain.post.FlyState
import app.hoop.domain.post.Reply
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.LoginIdentifier
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.User
import app.hoop.domain.identity.Username
import app.hoop.domain.post.PostDraft
import app.hoop.domain.post.PostId
import kotlinx.coroutines.flow.Flow

/** Ports (interfaces) to the outside world: implementations live in the data module. */
interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signIn(identifier: LoginIdentifier, password: Password): DomainResult<User>
    suspend fun signUp(request: SignUpRequest): DomainResult<User>
    suspend fun requestPasswordReset(email: Email): DomainResult<Unit>
    /** Changes the password of the signed-in user, who must give the current one. */
    suspend fun changePassword(current: Password, new: Password): DomainResult<Unit>
    suspend fun checkAvailability(username: Username?, email: Email?): DomainResult<Availability>
    suspend fun signOut()
    /**
     * Restores the session saved by a previous launch. Returns true if the user is still signed in
     * (also when the server cannot be reached, so the app opens offline), false if there is no saved
     * session or the server says it is no longer valid; in that case it is deleted.
     */
    suspend fun restoreSession(): Boolean
}

class SignUpRequest(
    val username: Username,
    val email: Email,
    val password: Password,
    val homeRadius: Radius,
    val location: GeoPoint?,
)

data class Availability(val usernameTaken: Boolean, val emailTaken: Boolean)

interface HoopsRepository {
    suspend fun getUserHoops(): DomainResult<UserHoops>
    suspend fun addHoop(name: String, center: GeoPoint, radius: Radius): DomainResult<UserHoops>
    suspend fun removeHoop(id: HoopId): DomainResult<UserHoops>
    suspend fun resizeHoop(id: HoopId, radius: Radius): DomainResult<UserHoops>
}

interface FeedRepository {
    /** Posts of [hoop] around [position] (the current position, for the initial Hoop), newest first. */
    suspend fun getFeed(hoop: Hoop, position: GeoPoint): DomainResult<List<FeedPost>>
    /** The replies of a post, oldest first. */
    suspend fun getReplies(postId: PostId): DomainResult<List<Reply>>
    suspend fun addReply(postId: PostId, text: String): DomainResult<Reply>
    /**
     * Sets the "Ci volo" of a post to the state the user wants ([flown]); answers with the state the server now has.
     * Sending the wanted state, not "flip it", makes a repeated request harmless.
     */
    suspend fun setFly(postId: PostId, flown: Boolean): DomainResult<FlyState>
    /** The same for a comment. */
    suspend fun setReplyFly(replyId: String, flown: Boolean): DomainResult<FlyState>
    suspend fun getNotifications(): DomainResult<List<AppNotification>>
}

/** The Home as it was the last time it loaded completely, with the moment it was saved. */
class HomeSnapshot(val hoops: UserHoops, val feeds: Map<HoopId, List<FeedPost>>, val savedAtEpochMillis: Long)

/**
 * Keeps the last complete Home on the device, so it can be shown when the server cannot be reached.
 * Replies are not kept: they are read when a post opens.
 */
interface HomeCacheRepository {
    /** The saved Home, with the age of its posts brought up to now; `null` if nothing was saved. */
    suspend fun load(): HomeSnapshot?
    /** Replaces the saved Home; answers with the moment it was saved. */
    suspend fun save(hoops: UserHoops, feeds: Map<HoopId, List<FeedPost>>): Long
    suspend fun clear()
}

interface PostsRepository {
    /** [onProgress] reports how much of the work is done, from 0 to 1 (the upload of a photo or video is most of it). */
    suspend fun publish(draft: PostDraft, onProgress: (Float) -> Unit = {}): DomainResult<PostId>
}

/**
 * Posts waiting to be sent, kept on the device so they survive the app being closed or the network being down.
 * One entry per attempt ([PostDraft.clientId]): adding the same attempt again replaces it.
 */
interface PendingPostsRepository {
    suspend fun add(draft: PostDraft)
    /** Oldest first. */
    suspend fun all(): List<PendingPost>
    suspend fun remove(clientId: String)
}

/** A post kept on the device, with how long it has been waiting. */
class PendingPost(val draft: PostDraft, val waitingMinutes: Int)

/**
 * Port to the device's reverse geocoder: the street of a position. The platform decides how it is found
 * (and which service sees the coordinates), the server never does it.
 */
fun interface AddressResolver {
    /** Short street name of [position], or `null` if it cannot be found (offline, no result). */
    suspend fun streetAt(position: GeoPoint): String?
}

/** Port to the device location: the platform decides how permission is asked and the position read. */
interface LocationProvider {
    suspend fun requestCurrentLocation(): DomainResult<GeoPoint>
}

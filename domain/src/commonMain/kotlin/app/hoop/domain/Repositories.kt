package app.hoop.domain

import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.Hoop
import app.hoop.domain.hoop.HoopId
import app.hoop.domain.hoop.UserHoops
import app.hoop.domain.identity.AppNotification
import app.hoop.domain.post.FeedPost
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
    suspend fun addReply(postId: PostId, text: String): DomainResult<Reply>
    suspend fun getNotifications(): DomainResult<List<AppNotification>>
}

interface PostsRepository {
    suspend fun publish(draft: PostDraft): DomainResult<PostId>
}

/** Port to the device location: the platform decides how permission is asked and the position read. */
interface LocationProvider {
    suspend fun requestCurrentLocation(): DomainResult<GeoPoint>
}

package app.hoop.domain

import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.hoop.UserHoops
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
    /** Completes a password reset started from the emailed link. */
    suspend fun confirmPasswordReset(newPassword: Password): DomainResult<Unit>
    suspend fun checkAvailability(username: Username?, email: Email?): DomainResult<Availability>
    suspend fun signOut()
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
}

interface PostsRepository {
    suspend fun publish(draft: PostDraft): DomainResult<PostId>
}

/** Port to the device location: the platform decides how permission is asked and the position read. */
interface LocationProvider {
    suspend fun requestCurrentLocation(): DomainResult<GeoPoint>
}

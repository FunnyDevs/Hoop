package app.hoop.presentation.onboarding

import app.hoop.domain.AuthRepository
import app.hoop.domain.Availability
import app.hoop.domain.LocationProvider
import app.hoop.domain.SignUpRequest
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.LoginIdentifier
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.User
import app.hoop.domain.identity.UserId
import app.hoop.domain.identity.Username
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory repository for ViewModel tests. */
class TestAuthRepository : AuthRepository {
    var takenEmails = setOf<String>()
    var takenUsernames = setOf<String>()
    var wrongLogin: String? = null
    var resetRequests = 0
    var confirmedPassword: String? = null

    override val currentUser: Flow<User?> = MutableStateFlow(null)

    override suspend fun signIn(identifier: LoginIdentifier, password: Password): DomainResult<User> =
        if (identifier.value == wrongLogin) DomainResult.failure(DomainError.InvalidCredentials)
        else DomainResult.success(user("demo", "demo@hoop.app"))

    override suspend fun signUp(request: SignUpRequest): DomainResult<User> =
        DomainResult.success(User(UserId("1"), request.username, request.email, request.username.value))

    override suspend fun requestPasswordReset(email: Email): DomainResult<Unit> {
        resetRequests++
        return DomainResult.success(Unit)
    }

    override suspend fun confirmPasswordReset(newPassword: Password): DomainResult<Unit> {
        confirmedPassword = newPassword.value
        return DomainResult.success(Unit)
    }

    override suspend fun checkAvailability(username: Username?, email: Email?): DomainResult<Availability> =
        DomainResult.success(Availability(username?.value in takenUsernames, email?.value in takenEmails))

    override suspend fun signOut() = Unit

    private fun user(name: String, mail: String): User {
        val u = (Username.create(name) as DomainResult.Success).value
        val e = (Email.create(mail) as DomainResult.Success).value
        return User(UserId("1"), u, e, name)
    }
}

class TestLocationProvider : LocationProvider {
    var result: DomainResult<GeoPoint> = GeoPoint.create(45.0, 9.0)
    override suspend fun requestCurrentLocation(): DomainResult<GeoPoint> = result
}

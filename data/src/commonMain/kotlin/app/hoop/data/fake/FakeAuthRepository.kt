package app.hoop.data.fake

import app.hoop.data.session.SessionTokenStore
import app.hoop.domain.AuthRepository
import app.hoop.domain.Availability
import app.hoop.domain.SignUpRequest
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.LoginIdentifier
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.User
import app.hoop.domain.identity.UserId
import app.hoop.domain.identity.Username
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake repository used until the server is configured: it lets the UI be tried out.
 * Reproduces the prototype cases (wrong credentials, email already used, username taken).
 */
internal class FakeAuthRepository(private val store: SessionTokenStore) : AuthRepository {
    private val user = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = user.asStateFlow()

    override suspend fun signIn(identifier: LoginIdentifier, password: Password): DomainResult<User> {
        delay(LATENCY_MILLIS)
        if (identifier.value == WRONG_LOGIN) return DomainResult.failure(DomainError.InvalidCredentials)
        store.save(DEMO_ID)
        return DomainResult.success(demoUser(identifier.value))
    }

    override suspend fun signUp(request: SignUpRequest): DomainResult<User> {
        delay(LATENCY_MILLIS)
        store.save(DEMO_ID)
        return DomainResult.success(User(UserId(DEMO_ID), request.username, request.email, request.username.value))
    }

    override suspend fun requestPasswordReset(email: Email): DomainResult<Unit> {
        delay(LATENCY_MILLIS)
        return DomainResult.success(Unit)
    }

    override suspend fun changePassword(current: Password, new: Password): DomainResult<Unit> {
        delay(LATENCY_MILLIS)
        return if (current.value == WRONG_PASSWORD) DomainResult.failure(DomainError.WrongPassword) else DomainResult.success(Unit)
    }

    override suspend fun checkAvailability(username: Username?, email: Email?): DomainResult<Availability> {
        delay(LATENCY_MILLIS)
        return DomainResult.success(
            Availability(
                usernameTaken = username?.value in TAKEN_USERNAMES,
                emailTaken = email?.value in TAKEN_EMAILS,
            ),
        )
    }

    override suspend fun signOut() {
        store.save(null)
        user.value = null
    }

    override suspend fun restoreSession(): Boolean = store.load() != null

    private fun demoUser(login: String): User {
        val name = Username.create(DEMO_USERNAME)
        val mail = Email.create(DEMO_EMAIL)
        require(name is DomainResult.Success && mail is DomainResult.Success)
        return User(UserId(DEMO_ID), name.value, mail.value, login)
    }

    private companion object {
        const val LATENCY_MILLIS = 600L
        const val DEMO_ID = "demo"
        const val DEMO_USERNAME = "demo"
        const val DEMO_EMAIL = "demo@hoop.app"
        const val WRONG_LOGIN = "errore@hoop.app"
        const val WRONG_PASSWORD = "errata"
        val TAKEN_USERNAMES = setOf("marta", "luca")
        val TAKEN_EMAILS = setOf("mario@hoop.app")
    }
}

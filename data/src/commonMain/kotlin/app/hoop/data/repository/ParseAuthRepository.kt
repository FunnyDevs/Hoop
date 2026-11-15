package app.hoop.data.repository

import app.hoop.data.HoopLog
import app.hoop.data.remote.ParseApiException
import app.hoop.data.remote.ParseAuthApi
import app.hoop.data.remote.ParseProtocol
import app.hoop.data.remote.SessionHolder
import app.hoop.data.remote.dto.ChangePasswordBody
import app.hoop.data.remote.dto.AvailabilityBody
import app.hoop.data.remote.dto.PasswordResetBody
import app.hoop.data.remote.dto.SignUpBody
import app.hoop.data.remote.dto.UserDto
import app.hoop.data.remote.toDomainError
import app.hoop.domain.AuthRepository
import app.hoop.domain.Availability
import app.hoop.domain.SignUpRequest
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.getOrNull
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.LoginIdentifier
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.User
import app.hoop.domain.identity.UserId
import app.hoop.domain.identity.Username
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

internal class ParseAuthRepository(private val api: ParseAuthApi, private val session: SessionHolder) : AuthRepository {
    private val user = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = user.asStateFlow()

    override suspend fun signIn(identifier: LoginIdentifier, password: Password): DomainResult<User> =
        call("signIn") {
            val dto = api.login(identifier.value, password.value)
            session.token = dto.sessionToken
            // /login does not return the email: it is read with the new session.
            (if (dto.email == null) fetchCurrentUser() else dto).toUser()
        }.also { it.getOrNull()?.let { u -> user.value = u } }

    override suspend fun signUp(request: SignUpRequest): DomainResult<User> =
        call("signUp") {
            val result = api.signUp(
                SignUpBody(
                    username = request.username.value,
                    email = request.email.value,
                    password = request.password.value,
                    radius = request.homeRadius.meters,
                    latitude = request.location?.latitude,
                    longitude = request.location?.longitude,
                ),
            )
            session.token = result.sessionToken
            result.user.toUser()
        }.also { it.getOrNull()?.let { u -> user.value = u } }

    override suspend fun requestPasswordReset(email: Email): DomainResult<Unit> =
        call("requestPasswordReset") { api.requestPasswordReset(PasswordResetBody(email.value)) }

    // The reset is completed on the web page that Parse serves from the link in the email.
    override suspend fun changePassword(current: Password, new: Password): DomainResult<Unit> =
        call("changePassword") {
            // The server closes the old sessions and returns a new one.
            val result = api.changePassword(ChangePasswordBody(current.value, new.value))
            session.token = result.sessionToken
        }

    override suspend fun checkAvailability(username: Username?, email: Email?): DomainResult<Availability> =
        call("checkAvailability") {
            val dto = api.checkAvailability(AvailabilityBody(username?.value, email?.value))
            Availability(dto.usernameTaken, dto.emailTaken)
        }

    override suspend fun signOut() {
        // Best effort: even if the server is unreachable the user is signed out on the device.
        runCatching { api.logout() }.onFailure { HoopLog.auth.w(it) { "signOut: logout request failed, signed out locally" } }
        session.token = null
        user.value = null
    }

    override suspend fun restoreSession(): Boolean {
        if (session.token == null) {
            HoopLog.auth.i { "restoreSession: no saved session" }
            return false
        }
        return try {
            val me = withTimeoutOrNull(RESTORE_TIMEOUT_MILLIS) { fetchCurrentUser() }
            me?.let { runCatching { it.toUser() }.getOrNull() }?.let { user.value = it }
            HoopLog.auth.i { if (me == null) "restoreSession: server too slow, keeping the session" else "restoreSession: session valid" }
            true
        } catch (e: ParseApiException) {
            if (e.code != ParseProtocol.ERROR_INVALID_SESSION) {
                HoopLog.auth.w { "restoreSession: server error ${e.code}, keeping the session" }
                return true
            }
            HoopLog.auth.i { "restoreSession: session revoked, signed out" }
            session.token = null
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Network error: keep the session, it will be checked again at the next launch.
            HoopLog.auth.w(e) { "restoreSession: server unreachable (${e::class.simpleName}: ${e.message}), keeping the session" }
            true
        }
    }

    private suspend fun <T> call(name: String, block: suspend () -> T): DomainResult<T> = try {
        DomainResult.success(block().also { HoopLog.auth.i { "$name: ok" } })
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        val error = e.toDomainError()
        // The cause is logged here: the domain error only says "network" for every technical failure.
        when (e) {
            is ParseApiException -> HoopLog.auth.w { "$name: server error ${e.code} \"${e.message}\" -> $error" }
            else -> HoopLog.auth.w(e) { "$name: failed (${e::class.simpleName}: ${e.message}) -> $error" }
        }
        DomainResult.failure(error)
    }

    /** /users/me validates the session; if the server leaves the email out, the cloud function adds it. */
    private suspend fun fetchCurrentUser(): UserDto {
        val me = api.currentUser()
        return if (me.email == null) api.currentUserWithEmail() else me
    }

    private fun UserDto.toUser(): User {
        val parsedUsername = Username.create(username).getOrNull() ?: throw IllegalStateException("$INVALID_PAYLOAD: username")
        val parsedEmail = email?.let { Email.create(it).getOrNull() } ?: throw IllegalStateException("$INVALID_PAYLOAD: email missing or invalid")
        return User(UserId(objectId), parsedUsername, parsedEmail, name ?: username)
    }

    private companion object {
        /** Do not keep the user on the splash screen when the server is slow. */
        const val RESTORE_TIMEOUT_MILLIS = 3_000L
        const val INVALID_PAYLOAD = "Invalid server response"
    }
}

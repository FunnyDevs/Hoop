package app.hoop.data.repository

import app.hoop.data.remote.ParseAuthApi
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ParseAuthRepository(private val api: ParseAuthApi) : AuthRepository {
    private val user = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = user.asStateFlow()

    override suspend fun signIn(identifier: LoginIdentifier, password: Password): DomainResult<User> =
        call { api.login(identifier.value, password.value).toUser() }.also { it.getOrNull()?.let { u -> user.value = u } }

    override suspend fun signUp(request: SignUpRequest): DomainResult<User> =
        call {
            api.signUp(
                SignUpBody(
                    username = request.username.value,
                    email = request.email.value,
                    password = request.password.value,
                    radius = request.homeRadius.meters,
                    latitude = request.location?.latitude,
                    longitude = request.location?.longitude,
                ),
            ).user.toUser()
        }.also { it.getOrNull()?.let { u -> user.value = u } }

    override suspend fun requestPasswordReset(email: Email): DomainResult<Unit> =
        call { api.requestPasswordReset(PasswordResetBody(email.value)) }

    // The reset-by-link completion endpoint is not available on the server yet.
    override suspend fun confirmPasswordReset(newPassword: Password): DomainResult<Unit> =
        DomainResult.failure(DomainError.Unknown)

    override suspend fun checkAvailability(username: Username?, email: Email?): DomainResult<Availability> =
        call {
            val dto = api.checkAvailability(AvailabilityBody(username?.value, email?.value))
            Availability(dto.usernameTaken, dto.emailTaken)
        }

    override suspend fun signOut() {
        user.value = null
    }


    private suspend fun <T> call(block: suspend () -> T): DomainResult<T> = try {
        DomainResult.success(block())
    } catch (e: Throwable) {
        DomainResult.failure(e.toDomainError())
    }

    private fun UserDto.toUser(): User {
        val parsedUsername = Username.create(username).getOrNull() ?: throw IllegalStateException(INVALID_PAYLOAD)
        val parsedEmail = Email.create(email).getOrNull() ?: throw IllegalStateException(INVALID_PAYLOAD)
        return User(UserId(objectId), parsedUsername, parsedEmail, name ?: username)
    }

    private companion object {
        const val INVALID_PAYLOAD = "Invalid server response"
    }
}

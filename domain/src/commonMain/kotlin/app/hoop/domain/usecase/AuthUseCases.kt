package app.hoop.domain.usecase

import app.hoop.domain.AuthRepository
import app.hoop.domain.Availability
import app.hoop.domain.SignUpRequest
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.flatMap
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius
import app.hoop.domain.identity.Email
import app.hoop.domain.identity.LoginIdentifier
import app.hoop.domain.identity.Password
import app.hoop.domain.identity.User
import app.hoop.domain.identity.Username

class SignInUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(identifier: String, password: String): DomainResult<User> =
        LoginIdentifier.create(identifier).flatMap { id ->
            auth.signIn(id, Password.ofExisting(password))
        }
}

class SignUpUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String,
        homeRadius: Radius,
        location: GeoPoint?,
    ): DomainResult<User> =
        Username.create(username).flatMap { u ->
            Email.create(email).flatMap { e ->
                Password.create(password).flatMap { p ->
                    auth.signUp(SignUpRequest(u, e, p, homeRadius, location))
                }
            }
        }
}

class RequestPasswordResetUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String): DomainResult<Unit> =
        Email.create(email).flatMap { auth.requestPasswordReset(it) }
}

class CheckEmailAvailabilityUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String): DomainResult<Availability> =
        Email.create(email).flatMap { auth.checkAvailability(username = null, email = it) }
}

class CheckUsernameAvailabilityUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(username: String): DomainResult<Availability> =
        Username.create(username).flatMap { auth.checkAvailability(username = it, email = null) }
}

class ConfirmPasswordResetUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(newPassword: String): DomainResult<Unit> =
        Password.create(newPassword).flatMap { auth.confirmPasswordReset(it) }
}

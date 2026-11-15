package app.hoop.data.remote

import app.hoop.domain.common.DomainError
import app.hoop.domain.common.Violation
import kotlinx.coroutines.CancellationException

/** Translates technical errors (Parse, network) into domain errors. */
internal fun Throwable.toDomainError(): DomainError = when {
    this is CancellationException -> throw this
    this is ParseApiException -> when (code) {
        ParseProtocol.ERROR_INVALID_LOGIN -> DomainError.InvalidCredentials
        ParseProtocol.ERROR_USERNAME_TAKEN -> DomainError.UsernameTaken
        ParseProtocol.ERROR_EMAIL_TAKEN -> DomainError.EmailAlreadyUsed
        ParseProtocol.ERROR_INVALID_EMAIL -> DomainError.Validation(Violation.EmailInvalid)
        ParseProtocol.ERROR_WRONG_PASSWORD -> DomainError.WrongPassword
        ParseProtocol.ERROR_INVALID_SESSION -> DomainError.Unauthorized
        else -> DomainError.Unknown
    }
    // The server answered but the payload is not what the app expects: not a connection problem.
    this is IllegalStateException -> DomainError.Unknown
    else -> DomainError.Network
}

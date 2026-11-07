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
        else -> DomainError.Unknown
    }
    else -> DomainError.Network
}

package app.hoop.domain.identity

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import kotlin.jvm.JvmInline

/** Value Object: what the user types to sign in, username or email. */
@JvmInline
value class LoginIdentifier private constructor(val value: String) {
    companion object {
        fun create(raw: String): DomainResult<LoginIdentifier> {
            val trimmed = raw.trim()
            return if (trimmed.isNotEmpty()) DomainResult.success(LoginIdentifier(trimmed))
            else DomainResult.failure(Violation.LoginIdentifierBlank)
        }
    }
}

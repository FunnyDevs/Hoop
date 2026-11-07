package app.hoop.domain.identity

import app.hoop.domain.common.AuthPolicy
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import kotlin.jvm.JvmInline

/** Value Object: username (letters, digits, dot and underscore). */
@JvmInline
value class Username private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        private val pattern = Regex(AuthPolicy.USERNAME_ALLOWED_PATTERN)

        fun create(raw: String): DomainResult<Username> {
            val trimmed = raw.trim()
            val valid = trimmed.length in AuthPolicy.USERNAME_MIN_LENGTH..AuthPolicy.USERNAME_MAX_LENGTH &&
                pattern.matches(trimmed)
            return if (valid) DomainResult.success(Username(trimmed)) else DomainResult.failure(Violation.UsernameInvalid)
        }
    }
}

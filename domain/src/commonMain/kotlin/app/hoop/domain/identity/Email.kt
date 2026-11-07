package app.hoop.domain.identity

import app.hoop.domain.common.AuthPolicy
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import kotlin.jvm.JvmInline

/** Value Object: valid, normalized email address (lowercase, no spaces). */
@JvmInline
value class Email private constructor(val value: String) {
    val domain: String get() = value.substringAfterLast('@')

    override fun toString(): String = value

    companion object {
        private val pattern = Regex(AuthPolicy.EMAIL_PATTERN)

        fun create(raw: String): DomainResult<Email> {
            val normalized = raw.trim().lowercase()
            return if (pattern.matches(normalized)) DomainResult.success(Email(normalized))
            else DomainResult.failure(Violation.EmailInvalid)
        }
    }
}

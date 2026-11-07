package app.hoop.domain.identity

import app.hoop.domain.common.AuthPolicy
import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import kotlin.jvm.JvmInline

/** Value Object: plain-text password. It never appears in logs: [toString] hides it. */
@JvmInline
value class Password private constructor(val value: String) {
    val strength: PasswordStrength get() = PasswordStrength.of(value)

    override fun toString(): String = "Password(***)"

    companion object {
        fun create(raw: String): DomainResult<Password> =
            if (raw.length >= AuthPolicy.PASSWORD_MIN_LENGTH) DomainResult.success(Password(raw))
            else DomainResult.failure(Violation.PasswordTooShort)

        /** For sign-in: the password is not validated here, the server verifies it. */
        fun ofExisting(raw: String): Password = Password(raw)
    }
}

enum class PasswordStrength {
    Empty, Weak, Fair, Good, Strong;

    companion object {
        /** Scores five independent criteria: length, long length, letters and digits, a symbol, mixed case. */
        fun of(raw: String): PasswordStrength {
            if (raw.isEmpty()) return Empty
            val criteria = listOf(
                raw.length >= AuthPolicy.PASSWORD_MIN_LENGTH,
                raw.length >= AuthPolicy.PASSWORD_STRONG_LENGTH,
                raw.any { it.isLetter() } && raw.any { it.isDigit() },
                raw.any { !it.isLetterOrDigit() },
                raw.any { it.isLowerCase() } && raw.any { it.isUpperCase() },
            )
            return when (criteria.count { it }) {
                0, 1 -> Weak
                2 -> Fair
                3 -> Good
                else -> Strong
            }
        }
    }
}

package app.hoop.domain.identity

import app.hoop.domain.assertSuccess
import app.hoop.domain.assertViolation
import app.hoop.domain.common.Violation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IdentityTest {
    @Test fun emailIsNormalized() = assertEquals("mario@hoop.app", Email.create("  Mario@Hoop.APP ").assertSuccess().value)
    @Test fun emailRejectsGarbage() = Email.create("mario@").assertViolation(Violation.EmailInvalid)
    @Test fun usernameRejectsShort() = Username.create("ab").assertViolation(Violation.UsernameInvalid)
    @Test fun usernameRejectsSymbols() = Username.create("ma rio!").assertViolation(Violation.UsernameInvalid)
    @Test fun usernameAccepts() = assertEquals("marta.rossi_1", Username.create("marta.rossi_1").assertSuccess().value)
    @Test fun passwordRejectsShort() = Password.create("abc").assertViolation(Violation.PasswordTooShort)
    @Test fun passwordIsNeverPrinted() = assertEquals("Password(***)", Password.create("supersegreta1").assertSuccess().toString())
    @Test fun strengthGrows() {
        assertEquals(PasswordStrength.Empty, PasswordStrength.of(""))
        assertEquals(PasswordStrength.Weak, PasswordStrength.of("abc"))
        assertEquals(PasswordStrength.Fair, PasswordStrength.of("hoop2026"))
        assertEquals(PasswordStrength.Good, PasswordStrength.of("hoop2026!"))
        assertEquals(PasswordStrength.Strong, PasswordStrength.of("Abcdefgh1234!"))
    }

    private val detector = EmailTypoDetector()
    @Test fun typoIsSuggested() = assertEquals("luca@gmail.com", detector.suggest(Email.create("luca@gmial.com").assertSuccess()))
    @Test fun knownDomainHasNoSuggestion() = assertNull(detector.suggest(Email.create("luca@gmail.com").assertSuccess()))
    @Test fun unrelatedDomainHasNoSuggestion() = assertNull(detector.suggest(Email.create("luca@azienda-xyz.it").assertSuccess()))
}

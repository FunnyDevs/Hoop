package app.hoop.domain

import app.hoop.domain.common.DomainResult
import app.hoop.domain.common.Violation
import app.hoop.domain.common.errorOrNull
import app.hoop.domain.common.DomainError
import kotlin.test.assertEquals
import kotlin.test.fail

fun <T> DomainResult<T>.assertSuccess(): T = when (this) {
    is DomainResult.Success -> value
    is DomainResult.Failure -> fail("Atteso successo, trovato $error")
}

fun <T> DomainResult<T>.assertViolation(expected: Violation) {
    assertEquals(DomainError.Validation(expected), errorOrNull())
}

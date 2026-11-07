package app.hoop.data

import app.hoop.data.remote.ParseApiException
import app.hoop.data.remote.ParseProtocol
import app.hoop.data.remote.toDomainError
import app.hoop.domain.common.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseErrorMapperTest {
    private fun map(code: Int) = ParseApiException(code, "").toDomainError()

    @Test fun invalidLogin() = assertEquals(DomainError.InvalidCredentials, map(ParseProtocol.ERROR_INVALID_LOGIN))
    @Test fun usernameTaken() = assertEquals(DomainError.UsernameTaken, map(ParseProtocol.ERROR_USERNAME_TAKEN))
    @Test fun emailTaken() = assertEquals(DomainError.EmailAlreadyUsed, map(ParseProtocol.ERROR_EMAIL_TAKEN))
    @Test fun unknownCode() = assertEquals(DomainError.Unknown, map(UNKNOWN_CODE))
    @Test fun technicalFailureIsNetwork() = assertEquals(DomainError.Network, RuntimeException().toDomainError())

    private companion object { const val UNKNOWN_CODE = 999 }
}

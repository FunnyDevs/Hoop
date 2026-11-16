package app.hoop.domain.common

/** Errors the domain can express. The presentation layer translates them into localized text. */
sealed interface DomainError {
    data class Validation(val violation: Violation) : DomainError
    data object InvalidCredentials : DomainError
    /** The current password given to change it is wrong. */
    data object WrongPassword : DomainError
    data object EmailAlreadyUsed : DomainError
    data object UsernameTaken : DomainError
    data object Unauthorized : DomainError
    data object LocationPermissionDenied : DomainError
    data object LocationUnavailable : DomainError
    data object Network : DomainError
    data object Unknown : DomainError
}

/** Domain rules violated by a value or an operation. */
enum class Violation {
    EmailInvalid,
    PasswordTooShort,
    UsernameInvalid,
    LoginIdentifierBlank,
    LatitudeOutOfRange,
    LongitudeOutOfRange,
    RadiusOutOfRange,
    PostTextBlank,
    PostTextTooLong,
    PostMediaMissing,
    /** The photo or video was not captured live by the in-app camera. */
    PostMediaNotLive,
    PostTypeNotAllowedInHoop,
    TooManyHoops,
    HomeHoopCannotBeRemoved,
    HoopNotFound,
}

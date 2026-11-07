package app.hoop.presentation.onboarding

import app.hoop.domain.common.DomainError
import app.hoop.domain.common.Violation
import app.hoop.presentation.onboarding.resources.*
import org.jetbrains.compose.resources.StringResource

/** Translates domain errors into localized text: the domain knows nothing about language. */
fun DomainError.messageRes(): StringResource = when (this) {
    is DomainError.Validation -> violation.messageRes()
    DomainError.InvalidCredentials -> Res.string.error_invalid_credentials
    DomainError.EmailAlreadyUsed -> Res.string.error_email_already_used
    DomainError.UsernameTaken -> Res.string.error_username_taken
    DomainError.Unauthorized -> Res.string.error_unauthorized
    DomainError.LocationPermissionDenied -> Res.string.error_location_permission_denied
    DomainError.LocationUnavailable -> Res.string.error_location_unavailable
    DomainError.Network -> Res.string.error_network
    DomainError.Unknown -> Res.string.error_unknown
}

fun Violation.messageRes(): StringResource = when (this) {
    Violation.EmailInvalid -> Res.string.violation_email_invalid
    Violation.PasswordTooShort -> Res.string.violation_password_too_short
    Violation.UsernameInvalid -> Res.string.violation_username_invalid
    Violation.LoginIdentifierBlank -> Res.string.violation_login_identifier_blank
    Violation.LatitudeOutOfRange, Violation.LongitudeOutOfRange -> Res.string.violation_coordinates_invalid
    Violation.RadiusOutOfRange -> Res.string.violation_radius_out_of_range
    Violation.PostTextBlank -> Res.string.violation_post_text_blank
    Violation.PostTextTooLong -> Res.string.violation_post_text_too_long
    Violation.PostMediaMissing -> Res.string.violation_post_media_missing
    Violation.PostTypeNotAllowedInHoop -> Res.string.violation_post_type_not_allowed
    Violation.TooManyHoops -> Res.string.violation_too_many_hoops
    Violation.HomeHoopCannotBeRemoved -> Res.string.violation_home_cannot_be_removed
    Violation.HoopNotFound -> Res.string.violation_hoop_not_found
}

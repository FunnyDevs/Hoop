package app.hoop.domain.common

/** Numeric business rules in one place, named. No magic numbers in the rest of the domain. */
object AuthPolicy {
    const val USERNAME_MIN_LENGTH = 3
    const val USERNAME_MAX_LENGTH = 30
    const val USERNAME_ALLOWED_PATTERN = "^[A-Za-z0-9._]+$"
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_STRONG_LENGTH = 12
    const val EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$"
    const val EMAIL_TYPO_MAX_DISTANCE = 2
    const val RESEND_RESET_AFTER_SECONDS = 20
}

object HoopPolicy {
    const val RADIUS_MIN_METERS = 200
    const val RADIUS_MAX_METERS = 20_000
    const val RADIUS_DEFAULT_METERS = 1_000
    const val MAX_EXTRA_HOOPS = 20
}

object PostPolicy {
    const val TEXT_MAX_LENGTH = 500
}

object GeoLimits {
    const val LATITUDE_MAX = 90.0
    const val LONGITUDE_MAX = 180.0
}

package app.hoop.presentation.onboarding

/** Switches and settings provided by the app to the onboarding screens. */
class OnboardingConfig(
    /** Style of the map shown while choosing the radius of the first Hoop. */
    val mapStyleUri: String,
    /** Map center used when the user has not shared a location. */
    val fallbackLatitude: Double,
    val fallbackLongitude: Double,
)

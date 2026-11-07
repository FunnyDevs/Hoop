package app.hoop.presentation.onboarding

/** Switches and settings provided by the app to the onboarding screens. */
class OnboardingConfig(
    /** Shows a button that simulates tapping the link of the reset email (useful when the backend is a mock). */
    val showResetLinkSimulator: Boolean,
    /** Style of the map shown while choosing the radius of the first Hoop. */
    val mapStyleUri: String,
    /** Map center used when the user has not shared a location. */
    val fallbackLatitude: Double,
    val fallbackLongitude: Double,
)

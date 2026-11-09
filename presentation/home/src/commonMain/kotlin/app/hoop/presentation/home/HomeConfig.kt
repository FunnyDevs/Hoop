package app.hoop.presentation.home

/** Settings provided by the app to the home screen. */
class HomeConfig(
    val mapStyleUri: String,
    /** Position used when the device location cannot be read. */
    val fallbackLatitude: Double,
    val fallbackLongitude: Double,
)

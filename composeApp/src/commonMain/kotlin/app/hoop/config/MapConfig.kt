package app.hoop.config

/** Map settings read from the platform: style URL and the fallback center. */
internal data class MapConfig(
    val styleUri: String,
    val fallbackLatitude: Double,
    val fallbackLongitude: Double,
)

internal expect fun platformMapConfig(): MapConfig

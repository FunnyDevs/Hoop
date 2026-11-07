package app.hoop.presentation.onboarding.common

import kotlin.math.roundToInt

private const val METERS_PER_KILOMETER = 1000
private const val METERS_PER_TENTH = 100
private const val DECIMAL_SEPARATOR = ','
private const val TENTHS_PER_UNIT = 10

/** Kilometers with at most one decimal and a comma separator (the app is Italian): 2500 → "2,5", 1000 → "1". */
internal fun formatKilometers(meters: Int): String {
    val tenths = (meters.toDouble() / METERS_PER_TENTH).roundToInt()
    val whole = tenths / TENTHS_PER_UNIT
    val fraction = tenths % TENTHS_PER_UNIT
    return if (fraction == 0) "$whole" else "$whole$DECIMAL_SEPARATOR$fraction"
}

internal fun isShorterThanKilometer(meters: Int): Boolean = meters < METERS_PER_KILOMETER

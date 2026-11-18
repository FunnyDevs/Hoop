package app.hoop.data.local

import kotlinx.datetime.Clock

/** Milliseconds since 1970 on the device clock. */
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

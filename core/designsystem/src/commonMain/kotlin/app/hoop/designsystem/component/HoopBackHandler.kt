package app.hoop.designsystem.component

import androidx.compose.runtime.Composable

/** Intercepts the system back gesture while [enabled]. No-op where the platform has no back gesture. */
@Composable
expect fun HoopBackHandler(enabled: Boolean, onBack: () -> Unit)

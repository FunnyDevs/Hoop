package app.hoop.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun HoopBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

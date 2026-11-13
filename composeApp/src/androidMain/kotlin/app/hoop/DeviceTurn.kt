package app.hoop

import android.view.OrientationEventListener
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.hoop.designsystem.component.HoopSystemBars
import app.hoop.designsystem.component.LocalDeviceTurn
import app.hoop.designsystem.component.LocalSystemBars
import app.hoop.designsystem.component.TURN_CLOCKWISE
import app.hoop.designsystem.component.TURN_COUNTER_CLOCKWISE

/** Angles (degrees, as reported by the sensor) around which the phone counts as turned; between them the last state stays. */
private object TurnAngles {
    const val UPRIGHT_MARGIN = 30
    const val LANDSCAPE_FROM = 60
    const val LANDSCAPE_TO = 120
    const val FULL_CIRCLE = 360
    const val OPPOSITE = 180
}

/**
 * Provides the physical turn of the phone (read from the sensor, so it works even with the activity locked upright and with
 * auto-rotate off) and the system bars controller to the screens below.
 */
@Composable
fun ProvideDeviceTurn(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val turn = remember { mutableIntStateOf(0) }
    val bars = remember {
        object : HoopSystemBars {
            override fun setHidden(hidden: Boolean) {
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                if (hidden) {
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }
    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                // The sensor angle grows clockwise: about 90 is the phone turned clockwise, about 270 counter-clockwise.
                val next = when {
                    orientation in TurnAngles.LANDSCAPE_FROM..TurnAngles.LANDSCAPE_TO -> TURN_CLOCKWISE
                    orientation in (TurnAngles.OPPOSITE + TurnAngles.LANDSCAPE_FROM)..(TurnAngles.OPPOSITE + TurnAngles.LANDSCAPE_TO) -> TURN_COUNTER_CLOCKWISE
                    orientation <= TurnAngles.UPRIGHT_MARGIN || orientation >= TurnAngles.FULL_CIRCLE - TurnAngles.UPRIGHT_MARGIN -> 0
                    else -> turn.intValue
                }
                if (next != turn.intValue) turn.intValue = next
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    CompositionLocalProvider(LocalDeviceTurn provides turn, LocalSystemBars provides bars, content = content)
}

package app.hoop.designsystem.component

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How the phone is physically turned, whatever the screen does (the app stays upright and animates the rotation itself):
 * 0 upright, [TURN_COUNTER_CLOCKWISE] with the top of the phone toward the left, [TURN_CLOCKWISE] toward the right.
 */
val LocalDeviceTurn = staticCompositionLocalOf<State<Int>> { UprightTurn }

const val TURN_COUNTER_CLOCKWISE = 1
const val TURN_CLOCKWISE = -1

private val UprightTurn: State<Int> = mutableIntStateOf(0)

/** Lets a screen hide and show the system bars (status and navigation) for a fully immersive view. */
interface HoopSystemBars {
    fun setHidden(hidden: Boolean)
}

val LocalSystemBars = staticCompositionLocalOf<HoopSystemBars> {
    object : HoopSystemBars {
        override fun setHidden(hidden: Boolean) = Unit
    }
}

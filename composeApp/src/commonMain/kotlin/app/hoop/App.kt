package app.hoop

import androidx.compose.runtime.Composable
import app.hoop.designsystem.theme.HoopTheme
import app.hoop.navigation.HoopNavHost

@Composable
fun App() {
    HoopTheme {
        HoopNavHost()
    }
}

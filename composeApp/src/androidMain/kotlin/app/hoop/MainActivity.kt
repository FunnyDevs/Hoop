package app.hoop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.hoop.location.ActivityPermissionRequester
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val permissionRequester: ActivityPermissionRequester by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionRequester.attach(this)
        setContent { App() }
    }

    override fun onDestroy() {
        permissionRequester.detach()
        super.onDestroy()
    }
}

package app.hoop.location

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Bridges runtime permission requests (which need an Activity) to suspending code.
 * The Activity calls [attach] in `onCreate` and [detach] in `onDestroy`.
 */
class ActivityPermissionRequester {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<Map<String, Boolean>>? = null

    fun attach(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            pending?.complete(result)
            pending = null
        }
    }

    fun detach() {
        launcher?.unregister()
        launcher = null
        pending?.complete(emptyMap())
        pending = null
    }

    /** Asks for [permissions]; returns what was granted. Empty if no Activity is available. */
    suspend fun request(permissions: Array<String>): Map<String, Boolean> {
        val activeLauncher = launcher ?: return emptyMap()
        val result = CompletableDeferred<Map<String, Boolean>>()
        pending = result
        activeLauncher.launch(permissions)
        return result.await()
    }
}

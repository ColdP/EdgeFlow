package btm.m.edgeflow

import android.app.Application
import btm.m.edgeflow.engine.PrivilegeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Annotated with @HiltAndroidApp to trigger Hilt's
 * code generation and create the application-level dependency container.
 *
 * On startup, initializes the [PrivilegeManager] (Shizuku listeners) and
 * performs a background root probe.
 */
@HiltAndroidApp
class EdgeFlowApp : Application() {

    @Inject lateinit var privilegeManager: PrivilegeManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Root-first: probe su → if no root, init Shizuku
        privilegeManager.init(this)
    }

    override fun onTerminate() {
        privilegeManager.release()
        super.onTerminate()
    }
}
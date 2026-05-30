package btm.m.edgeflow.engine

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard Intent-based app execution engine.
 *
 * Uses PackageManager.getLaunchIntentForPackage() to launch apps via standard Android Intent.
 * This is the most reliable method across all devices and SELinux configurations.
 *
 * System commands (svc wifi, etc.) still go through PrivilegeManager shell execution on IO thread.
 */
@Singleton
class ActionExecutor @Inject constructor(
    private val privilegeManager: PrivilegeManager
) {
    companion object {
        private const val TAG = "ActionExecutor"
    }

    /**
     * Launches an app using standard PackageManager Intent.
     * MUST be called with a Context that can start activities (Service or Activity).
     *
     * Uses FLAG_ACTIVITY_NEW_TASK (required from Service) + FLAG_ACTIVITY_CLEAR_TOP
     * to bring existing task to front if already running.
     *
     * This is a synchronous call on the calling thread — should be called from a coroutine
     * or directly from a click handler (it's fast, just an Intent dispatch).
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                Log.d(TAG, "App launched: $packageName")
                true
            } else {
                Log.e(TAG, "No launch intent found for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch: $packageName", e)
            false
        }
    }

    /**
     * Launches a quick-pay shortcut. Tries scheme URI first, then falls back to shell.
     */
    fun launchShortcut(context: Context, actionType: String, actionTarget: String): Boolean {
        return when (actionType) {
            "scheme" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(actionTarget))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Scheme launch failed: $actionTarget", e)
                    false
                }
            }
            "shell" -> {
                // Execute shell command synchronously (blocking)
                try {
                    // Note: executeShellCommand is suspend but we're in a non-suspend context.
                    // Use runBlocking as a bridge — this is acceptable for shell shortcuts.
                    kotlinx.coroutines.runBlocking {
                        privilegeManager.executeShellCommand(actionTarget)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Shell shortcut failed: $actionTarget", e)
                    false
                }
            }
            else -> false
        }
    }

    /**
     * Executes a system toggle command via privileged shell on IO thread.
     */
    suspend fun executeSystemCommand(shellCommand: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = privilegeManager.executeShellCommand(shellCommand)
            if (!success) {
                Log.e(TAG, "System command failed: $shellCommand")
            }
            success
        } catch (e: Throwable) {
            Log.e(TAG, "System command exception: $shellCommand", e)
            false
        }
    }
}
package btm.m.edgeflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import btm.m.edgeflow.service.EdgeTriggerService

/**
 * Listens for app upgrade/reinstall events and restarts the edge trigger service.
 * Prevents the sidebar from being disabled after an over-the-air update.
 */
class AppUpgradeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppUpgradeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "App updated – restarting EdgeTriggerService")
            try {
                EdgeTriggerService.start(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service after upgrade", e)
            }
        }
    }
}
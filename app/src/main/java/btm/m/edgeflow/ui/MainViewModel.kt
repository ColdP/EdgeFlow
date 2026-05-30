package btm.m.edgeflow.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import btm.m.edgeflow.service.EdgeTriggerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 *
 * Tracks overlay permission, notification listener permission,
 * service running state, and exposes combined [uiState] for the Compose UI.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val context get() = getApplication<Application>()

    // ── Overlay permission ───────────────────────────────────────────────────

    private val _hasOverlayPermission = MutableStateFlow(Settings.canDrawOverlays(context))
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    fun refreshOverlayPermission() {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
    }

    fun overlayPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    // ── Notification Listener permission ─────────────────────────────────────

    private val _hasNotificationListenerPermission = MutableStateFlow(false)
    val hasNotificationListenerPermission: StateFlow<Boolean> = _hasNotificationListenerPermission.asStateFlow()

    fun checkNotificationListenerPermission(context: android.content.Context) {
        try {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            val myComponent = ComponentName(context, btm.m.edgeflow.service.MediaListenerService::class.java).flattenToString()
            _hasNotificationListenerPermission.value = flat != null && flat.contains(myComponent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check notification listener permission", e)
            _hasNotificationListenerPermission.value = false
        }
    }

    // ── Service state ────────────────────────────────────────────────────────

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    /**
     * Combined UI state — recomposes when any tracked state changes.
     */
    val uiState: StateFlow<UiState> = combine(
        _hasOverlayPermission, _isServiceRunning, _hasNotificationListenerPermission
    ) { hasOverlay, isRunning, hasNotificationListener ->
        UiState(
            hasOverlayPermission = hasOverlay,
            isServiceRunning = isRunning,
            hasNotificationListenerPermission = hasNotificationListener
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(
            hasOverlayPermission = Settings.canDrawOverlays(context),
            isServiceRunning = false,
            hasNotificationListenerPermission = false
        )
    )

    // ── Service control ──────────────────────────────────────────────────────

    fun startService(side: Int = EdgeTriggerService.SIDE_LEFT) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot start service: overlay permission not granted")
            return
        }
        try {
            EdgeTriggerService.start(context, side)
            _isServiceRunning.value = true
            Log.d(TAG, "Service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
        }
    }

    fun stopService() {
        try {
            EdgeTriggerService.stop(context)
            _isServiceRunning.value = false
            Log.d(TAG, "Service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop service", e)
        }
    }

    fun toggleService(side: Int = EdgeTriggerService.SIDE_LEFT) {
        if (_isServiceRunning.value) stopService() else startService(side)
    }
}

data class UiState(
    val hasOverlayPermission: Boolean,
    val isServiceRunning: Boolean,
    val hasNotificationListenerPermission: Boolean = false
)
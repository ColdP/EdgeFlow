package btm.m.edgeflow.service

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationListenerService that listens for active media sessions
 * and exposes current media info (title, artist, isPlaying, album art) via static StateFlows.
 *
 * Uses MediaSessionManager to query active media sessions — this requires
 * BIND_NOTIFICATION_LISTENER_SERVICE permission (granted via Settings > Notification access).
 *
 * Media control is done via MediaController.transportControls (framework API),
 * which dispatches media key events to the active session.
 */
class MediaListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaListenerService"

        /** Static StateFlows that any ViewModel can collect. */
        private val _title = MutableStateFlow("")
        val title: StateFlow<String> = _title.asStateFlow()

        private val _artist = MutableStateFlow("")
        val artist: StateFlow<String> = _artist.asStateFlow()

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private val _albumArt = MutableStateFlow<Bitmap?>(null)
        val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

        /** Tracks whether the service is currently bound/alive. */
        private val _isServiceAlive = MutableStateFlow(false)
        val isServiceAlive: StateFlow<Boolean> = _isServiceAlive.asStateFlow()

        /** Reference to the active media controller (for transport controls). */
        @Volatile
        private var activeController: MediaController? = null

        /**
         * Dispatch media transport commands to the active session.
         * Requires the service to be alive and a session to be active.
         */
        fun dispatchMediaAction(action: String) {
            val controller = activeController
            if (controller == null) {
                Log.w(TAG, "No active media controller, cannot dispatch: $action")
                return
            }
            try {
                when (action) {
                    "play" -> controller.transportControls.play()
                    "pause" -> controller.transportControls.pause()
                    "toggle" -> {
                        if (_isPlaying.value) controller.transportControls.pause()
                        else controller.transportControls.play()
                    }
                    "next" -> controller.transportControls.skipToNext()
                    "previous" -> controller.transportControls.skipToPrevious()
                    "stop" -> controller.transportControls.stop()
                    else -> Log.w(TAG, "Unknown media action: $action")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch media action: $action", e)
            }
        }

        /** Clear media state when service is destroyed. */
        private fun clearState() {
            _title.value = ""
            _artist.value = ""
            _isPlaying.value = false
            _albumArt.value = null
            activeController = null
        }
    }

    private var mediaSessionManager: MediaSessionManager? = null

    private val sessionListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
            Log.d(TAG, "Active sessions changed: ${controllers?.size ?: 0}")
            attachToController(controllers?.firstOrNull())
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MediaListenerService created")
        _isServiceAlive.value = true

        try {
            mediaSessionManager = getSystemService(MediaSessionManager::class.java)
            // Must pass ComponentName to getActiveSessions — null requires MEDIA_CONTENT_CONTROL permission
            val myComponent = ComponentName(this, MediaListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, myComponent)

            // Attach to the currently active session immediately
            val controllers = mediaSessionManager?.getActiveSessions(myComponent)
            Log.d(TAG, "Initial active sessions: ${controllers?.size ?: 0}")
            attachToController(controllers?.firstOrNull())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: notification listener permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaSessionManager", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "MediaListenerService destroyed")
        _isServiceAlive.value = false
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing session listener", e)
        }
        mediaSessionManager = null
        clearState()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
        _isServiceAlive.value = true

        // Re-attach to active sessions after reconnection
        try {
            val myComponent = ComponentName(this, MediaListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(myComponent)
            attachToController(controllers?.firstOrNull())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-attach after listener connected", e)
        }
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "NotificationListener disconnected")
        _isServiceAlive.value = false
        super.onListenerDisconnected()
    }

    /**
     * Attach a [MediaController.Callback] to the given controller to receive
     * metadata and playback state changes in real time.
     */
    private fun attachToController(controller: MediaController?) {
        // Detach old callback
        activeController?.let { old ->
            try {
                old.unregisterCallback(controllerCallback)
            } catch (_: Exception) {}
        }

        activeController = controller

        if (controller == null) {
            Log.d(TAG, "No active media controller")
            clearState()
            return
        }

        Log.d(TAG, "Attaching to controller: ${controller.packageName}")

        try {
            controller.registerCallback(controllerCallback)
            // Read current metadata immediately
            updateMetadata(controller.metadata)
            updatePlaybackState(controller.playbackState)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register controller callback", e)
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "Metadata changed: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}")
            updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Playback state changed: ${state?.state}")
            updatePlaybackState(state)
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "Media session destroyed")
            activeController = null
            clearState()
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata == null) {
            _title.value = ""
            _artist.value = ""
            _albumArt.value = null
            return
        }
        _title.value = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        _artist.value = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

        // Extract album art bitmap
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        _albumArt.value = art
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        _isPlaying.value = state?.state == PlaybackState.STATE_PLAYING
    }
}
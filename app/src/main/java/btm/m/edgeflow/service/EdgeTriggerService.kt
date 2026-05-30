package btm.m.edgeflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.app.NotificationCompat
import btm.m.edgeflow.MainActivity
import btm.m.edgeflow.R
import btm.m.edgeflow.data.UserPreferences
import btm.m.edgeflow.data.dao.CustomLinkDao
import btm.m.edgeflow.data.dao.SelectedAppDao
import btm.m.edgeflow.engine.ActionExecutor
import btm.m.edgeflow.engine.PrivilegeManager
import btm.m.edgeflow.overlay.OverlayManager
import btm.m.edgeflow.sidebar.SidebarScreen
import btm.m.edgeflow.sidebar.SidebarViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class EdgeTriggerService : Service() {

    companion object {
        private const val TAG = "EdgeTriggerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "edgeflow_service_channel"
        const val EXTRA_SIDE = "extra_side"
        const val SIDE_LEFT = 0
        const val SIDE_RIGHT = 1

        fun start(context: Context, side: Int = SIDE_LEFT) {
            val intent = Intent(context, EdgeTriggerService::class.java).apply {
                putExtra(EXTRA_SIDE, side)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EdgeTriggerService::class.java))
        }
    }

    private var edgeSide: Int = SIDE_LEFT
    private lateinit var overlayManager: OverlayManager

    @Inject lateinit var privilegeManager: PrivilegeManager
    @Inject lateinit var actionExecutor: ActionExecutor
    @Inject lateinit var selectedAppDao: SelectedAppDao
    @Inject lateinit var customLinkDao: CustomLinkDao

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sidebarViewModel: SidebarViewModel

    private var headerImageUri: String? = null
    private var sidebarWidthRatio: Float = 0.70f

    private var triggerMethod: Int = 0
    private var triggerVibrate: Boolean = true
    private var triggerPosition: Int = 0
    private var triggerWidth: Float = 24f
    private var triggerHeight: Float = 0.6f
    private var triggerYOffset: Float = 0f
    private var triggerSensitivity: Float = 40f
    private var triggerDebug: Boolean = false

    private var vibrator: Vibrator? = null
    private val dismissTrigger = mutableIntStateOf(0)

    private data class TriggerConfig(
        val method: Int, val vibrate: Boolean, val position: Int,
        val width: Float, val height: Float, val yOffset: Float,
        val sensitivity: Float, val debug: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        overlayManager = OverlayManager(applicationContext)
        sidebarViewModel = SidebarViewModel(selectedAppDao, customLinkDao, actionExecutor, privilegeManager)
        createNotificationChannel()
        privilegeManager.init(applicationContext)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        serviceScope.launch {
            UserPreferences.headerImageUriFlow(applicationContext).collect { headerImageUri = it }
        }
        serviceScope.launch {
            UserPreferences.sidebarWidthRatioFlow(applicationContext).collect {
                sidebarWidthRatio = it; sidebarViewModel.updatePanelWidthFraction(it)
            }
        }
        serviceScope.launch {
            UserPreferences.blurRadiusFlow(applicationContext).collect { sidebarViewModel.updateConfig(blurRadius = it) }
        }
        serviceScope.launch {
            UserPreferences.panelAlphaFlow(applicationContext).collect { sidebarViewModel.updateConfig(panelAlpha = it) }
        }
        serviceScope.launch {
            UserPreferences.bgImageUriFlow(applicationContext).collect { sidebarViewModel.updateConfig(bgImageUri = it ?: "") }
        }
        serviceScope.launch {
            UserPreferences.bgImageAlphaFlow(applicationContext).collect { sidebarViewModel.updateConfig(bgImageAlpha = it) }
        }
        serviceScope.launch {
            UserPreferences.bgImageBlurFlow(applicationContext).collect { sidebarViewModel.updateConfig(bgImageBlur = it) }
        }

        // Trigger settings hot-reload via combined Flow
        serviceScope.launch {
            combine(
                UserPreferences.triggerMethodFlow(applicationContext),
                UserPreferences.triggerVibrateFlow(applicationContext),
                UserPreferences.triggerPositionFlow(applicationContext),
                UserPreferences.triggerWidthFlow(applicationContext),
                UserPreferences.triggerHeightFlow(applicationContext),
                UserPreferences.triggerYOffsetFlow(applicationContext),
                UserPreferences.triggerSensitivityFlow(applicationContext),
                UserPreferences.triggerDebugFlow(applicationContext)
            ) { values: Array<Any?> ->
                TriggerConfig(
                    method = values[0] as Int, vibrate = values[1] as Boolean,
                    position = values[2] as Int, width = values[3] as Float,
                    height = values[4] as Float, yOffset = values[5] as Float,
                    sensitivity = values[6] as Float, debug = values[7] as Boolean
                )
            }.collect { cfg ->
                val positionChanged = cfg.position != triggerPosition
                triggerMethod = cfg.method; triggerVibrate = cfg.vibrate
                triggerPosition = cfg.position; triggerWidth = cfg.width
                triggerHeight = cfg.height; triggerYOffset = cfg.yOffset
                triggerSensitivity = cfg.sensitivity; triggerDebug = cfg.debug

                // MUST dispatch to Main thread for WindowManager operations
                withContext(Dispatchers.Main) {
                    try {
                        if (::overlayManager.isInitialized && overlayManager.getEdgeTriggerView() != null) {
                            if (positionChanged) {
                                overlayManager.removeEdgeTriggerStrip()
                                attachEdgeTrigger()
                            } else {
                                updateTriggerLayoutParams()
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to hot-reload trigger", e)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        edgeSide = intent?.getIntExtra(EXTRA_SIDE, SIDE_LEFT) ?: SIDE_LEFT
        startForeground(NOTIFICATION_ID, buildNotification())
        attachEdgeTrigger()
        Log.d(TAG, "Service started, edgeSide=$edgeSide")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        serviceScope.cancel()
        overlayManager.removeAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun attachEdgeTrigger() {
        try {
            val isRight = triggerPosition == 1
            val gravity = if (isRight) Gravity.RIGHT else Gravity.LEFT
            val density = resources.displayMetrics.density
            val extraRightPx = if (isRight) (10 * density).toInt() else 0
            val widthPx = (triggerWidth * density).toInt() + extraRightPx
            val screenHeightPx = resources.displayMetrics.heightPixels
            val zoneHeightPx = (screenHeightPx * triggerHeight).toInt()
            val yOffsetPx = (screenHeightPx * triggerYOffset).toInt()

            overlayManager.addEdgeTriggerStrip(
                side = gravity, widthPx = widthPx, heightPx = zoneHeightPx,
                yOffsetPx = yOffsetPx, sensitivityPx = triggerSensitivity.toInt(),
                highlightDebug = triggerDebug,
                onGestureDetected = { onEdgeGestureDetected() }
            )

            overlayManager.getEdgeTriggerView()?.apply {
                isRightSide = isRight
                swipeThresholdPx = triggerSensitivity.toInt()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "attachEdgeTrigger failed", e)
        }
    }

    private fun updateTriggerLayoutParams() {
        try {
            val params = overlayManager.getEdgeTriggerParams() ?: return
            val isRight = triggerPosition == 1
            val density = resources.displayMetrics.density
            val extraRightPx = if (isRight) (10 * density).toInt() else 0
            params.width = (triggerWidth * density).toInt() + extraRightPx
            params.height = (resources.displayMetrics.heightPixels * triggerHeight).toInt()
            params.y = (resources.displayMetrics.heightPixels * triggerYOffset).toInt()

            overlayManager.getEdgeTriggerView()?.apply {
                setBackgroundColor(if (triggerDebug) 0x55FF0000.toInt() else 0x00000000)
                swipeThresholdPx = triggerSensitivity.toInt()
            }
            overlayManager.updateEdgeTriggerLayoutParams(params)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to hot-reload trigger params", e)
        }
    }

    private fun vibrate() {
        if (!triggerVibrate) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator?.vibrate(50)
            }
        } catch (e: Exception) { Log.w(TAG, "Vibration failed", e) }
    }

    private fun onEdgeGestureDetected() {
        if (overlayManager.isSidebarShowing()) return
        Log.d(TAG, "Edge gesture detected – method=$triggerMethod")
        vibrate()
        dismissTrigger.intValue = 0
        sidebarViewModel.show()
        sidebarViewModel.initialize(applicationContext)
        sidebarViewModel.registerSystemStateReceivers(applicationContext)

        overlayManager.addSidebarOverlay(
            onBackPressIntercepted = {
                Log.d(TAG, "Back key intercepted – triggering dismiss")
                dismissTrigger.intValue++
            }
        ) {
            btm.m.edgeflow.ui.theme.EdgeFlowTheme { SidebarContent() }
        }
        overlayManager.setSidebarFocusable(true)
    }

    @Composable
    private fun SidebarContent() {
        val config by sidebarViewModel.config.collectAsState()
        val components by sidebarViewModel.components.collectAsState()
        val cardConfigs by sidebarViewModel.cardConfigs.collectAsState()
        val customLinks by sidebarViewModel.customLinks.collectAsState()
        val currentDismissTrigger = dismissTrigger.intValue

        SidebarScreen(
            config = config, components = components, cardConfigs = cardConfigs,
            customLinks = customLinks,
            headerImageUri = headerImageUri, dismissTrigger = currentDismissTrigger,
            onRemoveOverlay = {
                sidebarViewModel.hide(); overlayManager.setSidebarFocusable(false)
                overlayManager.removeSidebarOverlay()
            },
            onAppClick = { app ->
                sidebarViewModel.launchApp(applicationContext, app.packageName)
                dismissTrigger.intValue++
            },
            onAppLongPress = { app -> sidebarViewModel.removeApp(app.packageName) },
            onSystemToggled = { control, enabled ->
                sidebarViewModel.toggleSystemControl(applicationContext, control, enabled)
            },
            onAddApp = {
                val pickerIntent = Intent(applicationContext, btm.m.edgeflow.AppPickerActivity::class.java)
                pickerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(pickerIntent)
            },
            onRefreshSystemState = { sidebarViewModel.refreshSystemState(applicationContext) },
            onBlurRadiusChanged = { radius ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        overlayManager.getSidebarLayoutParams()?.let {
                            it.blurBehindRadius = radius; overlayManager.updateSidebarLayoutParams(it)
                        }
                    } catch (_: Exception) {}
                }
            },
            onMediaPlayPause = { sidebarViewModel.mediaPlayPause() },
            onMediaNext = { sidebarViewModel.mediaNext() },
            onMediaPrevious = { sidebarViewModel.mediaPrevious() },
            onShortcutClick = { shortcut ->
                sidebarViewModel.launchShortcut(applicationContext, shortcut.actionType, shortcut.actionTarget)
            },
            onCustomLinkClick = { url ->
                sidebarViewModel.launchCustomLink(applicationContext, url)
                dismissTrigger.intValue++
            }
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "EdgeFlow Service", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Keeps the edge gesture listener active"
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EdgeFlow is active")
            .setContentText("Swipe from the screen edge to open the sidebar")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()
    }
}
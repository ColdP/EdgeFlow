package btm.m.edgeflow.sidebar

import android.app.AppOpsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import btm.m.edgeflow.data.UserPreferences
import btm.m.edgeflow.data.dao.CustomLinkDao
import btm.m.edgeflow.data.dao.SelectedAppDao
import btm.m.edgeflow.data.entity.CustomLink
import btm.m.edgeflow.data.entity.SelectedApp
import btm.m.edgeflow.engine.ActionExecutor
import btm.m.edgeflow.engine.PrivilegeManager
import btm.m.edgeflow.service.MediaListenerService
import btm.m.edgeflow.sidebar.model.AppShortcut
import btm.m.edgeflow.sidebar.model.CardConfig
import btm.m.edgeflow.sidebar.model.CardConfigManager
import btm.m.edgeflow.sidebar.model.ControlType
import btm.m.edgeflow.sidebar.model.MediaCard
import btm.m.edgeflow.sidebar.model.PayShortcut
import btm.m.edgeflow.sidebar.model.SectionHeader
import btm.m.edgeflow.sidebar.model.ShortcutCard
import btm.m.edgeflow.sidebar.model.SidebarComponent
import btm.m.edgeflow.sidebar.model.SystemControl
import btm.m.edgeflow.sidebar.model.toShellCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SidebarViewModel"

@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val selectedAppDao: SelectedAppDao,
    private val customLinkDao: CustomLinkDao,
    private val actionExecutor: ActionExecutor,
    private val privilegeManager: PrivilegeManager
) : ViewModel() {

    // ── Config ───────────────────────────────────────────────────────────

    private val _config = MutableStateFlow(SidebarConfig())
    val config: StateFlow<SidebarConfig> = _config.asStateFlow()

    fun updateConfig(
        blurRadius: Float? = null, panelAlpha: Float? = null,
        panelWidthFraction: Float? = null, bgImageUri: String? = null,
        bgImageAlpha: Float? = null, bgImageBlur: Float? = null
    ) {
        _config.update {
            it.copy(
                blurRadius = blurRadius ?: it.blurRadius,
                maskAlpha = panelAlpha ?: it.maskAlpha,
                panelWidthFraction = panelWidthFraction ?: it.panelWidthFraction,
                backgroundUri = bgImageUri ?: it.backgroundUri,
                bgImageAlpha = bgImageAlpha ?: it.bgImageAlpha,
                bgImageBlur = bgImageBlur ?: it.bgImageBlur
            )
        }
    }

    fun updatePanelWidthFraction(fraction: Float) {
        _config.update { it.copy(panelWidthFraction = fraction.coerceIn(0.5f, 1.0f)) }
    }

    // ── Visibility ───────────────────────────────────────────────────────

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()
    fun show() { _isVisible.value = true }
    fun hide() { _isVisible.value = false }

    // ── System real-time state ────────────────────────────────────────────

    private val _wifiEnabled = MutableStateFlow(false)
    private val _bluetoothEnabled = MutableStateFlow(false)
    private val _dndEnabled = MutableStateFlow(false)
    private val _rotationEnabled = MutableStateFlow(true)
    private val _mobileDataEnabled = MutableStateFlow(true)
    private val _gpsEnabled = MutableStateFlow(false)

    // ── Media state ───────────────────────────────────────────────────────

    private val _mediaTitle = MutableStateFlow("")
    private val _mediaArtist = MutableStateFlow("")
    private val _mediaIsPlaying = MutableStateFlow(false)
    private val _mediaAlbumArt = MutableStateFlow<Bitmap?>(null)

    // ── Card configs ──────────────────────────────────────────────────────

    private val _cardConfigs = MutableStateFlow(CardConfigManager.DEFAULT_CONFIGS)
    val cardConfigs: StateFlow<List<CardConfig>> = _cardConfigs.asStateFlow()

    // ── Selected apps ─────────────────────────────────────────────────────

    val customLinks: StateFlow<List<CustomLink>> = customLinkDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedApps: StateFlow<List<SelectedApp>> = selectedAppDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Combined component list ───────────────────────────────────────────

    val components: StateFlow<List<SidebarComponent>> = combine(
        selectedApps, _wifiEnabled, _bluetoothEnabled,
        _dndEnabled, _rotationEnabled, _mobileDataEnabled, _gpsEnabled,
        _mediaTitle, _mediaArtist, _mediaIsPlaying, _mediaAlbumArt
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        buildComponentList(
            apps = array[0] as List<SelectedApp>,
            wifiEnabled = array[1] as Boolean,
            btEnabled = array[2] as Boolean,
            dndEnabled = array[3] as Boolean,
            rotationEnabled = array[4] as Boolean,
            mobileDataEnabled = array[5] as Boolean,
            gpsEnabled = array[6] as Boolean,
            mediaTitle = array[7] as String,
            mediaArtist = array[8] as String,
            mediaPlaying = array[9] as Boolean,
            albumArt = array[10] as? Bitmap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── All installed apps ────────────────────────────────────────────────

    private val _allInstalledApps = MutableStateFlow<List<AppShortcut>>(emptyList())
    val allInstalledApps: StateFlow<List<AppShortcut>> = _allInstalledApps.asStateFlow()

    // ── Card config management ────────────────────────────────────────────

    fun loadCardConfigs(context: Context) {
        viewModelScope.launch {
            CardConfigManager.configFlow(context).collect { _cardConfigs.value = it }
        }
    }

    fun toggleCardVisibility(context: Context, cardId: String) {
        viewModelScope.launch { CardConfigManager.toggleVisibility(context, cardId) }
    }

    fun moveCard(context: Context, cardId: String, direction: Int) {
        viewModelScope.launch { CardConfigManager.moveCard(context, cardId, direction) }
    }

    // ── Shortcut launching ────────────────────────────────────────────────

    fun launchShortcut(context: Context, actionType: String, actionTarget: String) {
        try { actionExecutor.launchShortcut(context, actionType, actionTarget) }
        catch (e: Throwable) { Log.e(TAG, "launchShortcut failed", e) }
    }

    // ── App launch ────────────────────────────────────────────────────────

    fun launchApp(context: Context, packageName: String) {
        try { actionExecutor.launchApp(context, packageName) }
        catch (e: Throwable) { Log.e(TAG, "launchApp failed: $packageName", e) }
    }

    // ── Media control ─────────────────────────────────────────────────────

    fun mediaPlayPause() { viewModelScope.launch { MediaListenerService.dispatchMediaAction("toggle") } }
    fun mediaNext() { viewModelScope.launch { MediaListenerService.dispatchMediaAction("next") } }
    fun mediaPrevious() { viewModelScope.launch { MediaListenerService.dispatchMediaAction("previous") } }

    // ── System control ────────────────────────────────────────────────────

    fun toggleSystemControl(context: Context, control: SystemControl, enabled: Boolean) {
        val shellCmd = control.type.toShellCommand(enabled) ?: return
        viewModelScope.launch {
            try { actionExecutor.executeSystemCommand(shellCmd) }
            catch (e: Throwable) { Log.e(TAG, "Shell exception: $shellCmd", e) }
            kotlinx.coroutines.delay(400)
            refreshSystemState(context)
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    fun addApp(packageName: String, label: String) {
        viewModelScope.launch {
            selectedAppDao.insert(SelectedApp(packageName = packageName, label = label, sortOrder = Int.MAX_VALUE))
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch { selectedAppDao.deleteByPackageName(packageName) }
    }

    fun launchCustomLink(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "launchCustomLink failed: $url", e)
        }
    }

    // ── Initialize ────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        checkUsagePermission(context)
        refreshSystemState(context)
        loadAllInstalledApps(context)
        collectMediaState()
        loadCardConfigs(context)

        viewModelScope.launch {
            if (selectedAppDao.count() == 0) {
                val defaults = getDefaultAppPackageNames(context)
                val pm = context.packageManager
                val entities = defaults.mapIndexedNotNull { idx, pkg ->
                    try {
                        val info = pm.getApplicationInfo(pkg, 0)
                        SelectedApp(packageName = pkg, label = pm.getApplicationLabel(info).toString(), sortOrder = idx)
                    } catch (_: Exception) { null }
                }
                if (entities.isNotEmpty()) selectedAppDao.insertAll(entities)
            }
        }
    }

    private fun collectMediaState() {
        viewModelScope.launch { MediaListenerService.title.collect { _mediaTitle.value = it } }
        viewModelScope.launch { MediaListenerService.artist.collect { _mediaArtist.value = it } }
        viewModelScope.launch { MediaListenerService.isPlaying.collect { _mediaIsPlaying.value = it } }
        viewModelScope.launch { MediaListenerService.albumArt.collect { _mediaAlbumArt.value = it } }
    }

    // ── System state refresh — BULLETPROOF ────────────────────────────────

    @Suppress("DEPRECATION")
    fun refreshSystemState(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // WiFi
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    _wifiEnabled.value = wm?.isWifiEnabled == true
                } catch (e: Throwable) {
                    Log.e(TAG, "WiFi read failed", e); _wifiEnabled.value = false
                }

                // Bluetooth
                try {
                    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    _bluetoothEnabled.value = bm?.adapter?.isEnabled == true
                } catch (e: Throwable) {
                    Log.e(TAG, "BT read failed", e); _bluetoothEnabled.value = false
                }

                // Rotation
                try {
                    _rotationEnabled.value = Settings.System.getInt(
                        context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1
                    ) == 1
                } catch (e: Throwable) {
                    Log.e(TAG, "Rotation read failed", e); _rotationEnabled.value = true
                }

                // DND
                try {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    _dndEnabled.value = am?.ringerMode != AudioManager.RINGER_MODE_NORMAL
                } catch (e: Throwable) {
                    Log.e(TAG, "DND read failed", e); _dndEnabled.value = false
                }

                // GPS
                try {
                    val m = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                    _gpsEnabled.value = m != Settings.Secure.LOCATION_MODE_OFF
                } catch (e: Throwable) {
                    Log.e(TAG, "GPS read failed", e); _gpsEnabled.value = false
                }

                // Mobile data
                _mobileDataEnabled.value = true

            } catch (e: Throwable) {
                // OUTER SAFETY NET — catches anything that escaped
                Log.e(TAG, "CRITICAL refreshSystemState outer catch", e)
                _wifiEnabled.value = false; _bluetoothEnabled.value = false
                _rotationEnabled.value = true; _dndEnabled.value = false
                _gpsEnabled.value = false; _mobileDataEnabled.value = false
            }
        }
    }

    // ── Permission ────────────────────────────────────────────────────────

    fun registerSystemStateReceivers(context: Context) {
        val filter = android.content.IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        _wifiEnabled.value = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED
                    }
                    android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        _bluetoothEnabled.value = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, android.bluetooth.BluetoothAdapter.STATE_OFF) == android.bluetooth.BluetoothAdapter.STATE_ON
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        refreshSystemState(context)
    }

    private fun checkUsagePermission(context: Context) {
        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
        } catch (e: Throwable) { Log.e(TAG, "Usage permission check failed", e) }
    }

    fun loadAllInstalledApps(context: Context) {
        viewModelScope.launch {
            val pm = context.packageManager
            val apps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                pm.queryIntentActivities(intent, 0)
                    .filter { it.activityInfo.packageName != context.packageName }
                    .distinctBy { it.activityInfo.packageName }
                    .mapNotNull { ri ->
                        try {
                            val pkg = ri.activityInfo.packageName
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            AppShortcut(id = "installed_${pkg.hashCode()}", packageName = pkg, label = pm.getApplicationLabel(appInfo).toString(), iconUri = "package:$pkg")
                        } catch (_: Exception) { null }
                    }.sortedBy { it.label.lowercase() }
            }
            _allInstalledApps.value = apps
        }
    }

    // ── Build component list ──────────────────────────────────────────────

    private fun buildComponentList(
        apps: List<SelectedApp>, wifiEnabled: Boolean, btEnabled: Boolean,
        dndEnabled: Boolean, rotationEnabled: Boolean, mobileDataEnabled: Boolean,
        gpsEnabled: Boolean, mediaTitle: String, mediaArtist: String,
        mediaPlaying: Boolean, albumArt: Bitmap? = null
    ): List<SidebarComponent> = buildList {
        if (apps.isNotEmpty()) {
            add(SectionHeader(id = "header_apps", title = "Apps"))
            apps.forEach { add(AppShortcut(id = "room_${it.packageName.hashCode()}", packageName = it.packageName, label = it.label, iconUri = "package:${it.packageName}")) }
        }
        add(SectionHeader(id = "header_shortcut", title = "Quick Actions"))
        add(ShortcutCard(id = "shortcut_card", shortcuts = buildDefaultShortcuts()))
        add(SectionHeader(id = "header_media", title = "Now Playing"))
        add(MediaCard(id = "media_card", title = if (mediaTitle.isBlank()) "No media playing" else mediaTitle, artist = mediaArtist, isPlaying = mediaPlaying, albumArt = albumArt))
        add(SectionHeader(id = "header_system", title = "Quick Controls"))
        add(SystemControl(id = "ctrl_wifi", type = ControlType.WIFI, label = "Wi-Fi", enabled = wifiEnabled))
        add(SystemControl(id = "ctrl_bt", type = ControlType.BLUETOOTH, label = "Bluetooth", enabled = btEnabled))
        add(SystemControl(id = "ctrl_data", type = ControlType.MOBILE_DATA, label = "Data", enabled = mobileDataEnabled))
        add(SystemControl(id = "ctrl_flash", type = ControlType.FLASHLIGHT, label = "Torch", enabled = false))
        add(SystemControl(id = "ctrl_dnd", type = ControlType.DO_NOT_DISTURB, label = "DND", enabled = dndEnabled))
        add(SystemControl(id = "ctrl_rotation", type = ControlType.ROTATION, label = "Rotation", enabled = rotationEnabled))
        add(SystemControl(id = "ctrl_gps", type = ControlType.GPS, label = "GPS", enabled = gpsEnabled))
        add(SystemControl(id = "ctrl_airplane", type = ControlType.AIRPLANE, label = "Airplane", enabled = false))
    }

    private fun buildDefaultShortcuts(): List<PayShortcut> = listOf(
        PayShortcut("WeChat Scan", "wechat_scan", "shell", "am start -n com.tencent.mm/com.tencent.mm.plugin.scanner.ui.BaseScanUI"),
        PayShortcut("WeChat Pay", "wechat_pay", "shell", "am start -n com.tencent.mm/com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI"),
        PayShortcut("Alipay Scan", "alipay_scan", "scheme", "alipayqr://platformapi/startapp?saId=10000007"),
        PayShortcut("Alipay Pay", "alipay_pay", "scheme", "alipayqr://platformapi/startapp?saId=20000056")
    )

    private fun getDefaultAppPackageNames(context: Context): List<String> {
        return listOf("com.android.settings", "com.android.camera", "com.android.browser", "com.android.contacts", "com.android.dialer", "com.android.mms", "com.android.calculator2", "com.android.calendar")
            .filter { try { context.packageManager.getApplicationInfo(it, 0); true } catch (_: Exception) { false } }
    }
}

data class SidebarConfig(
    val backgroundUri: String = "", val blurRadius: Float = 35f,
    val maskAlpha: Float = 0.45f, val panelWidthFraction: Float = 0.80f,
    val bgImageAlpha: Float = 0.5f, val bgImageBlur: Float = 15f
)
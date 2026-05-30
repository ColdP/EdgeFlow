package btm.m.edgeflow

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import btm.m.edgeflow.data.UserPreferences
import btm.m.edgeflow.engine.PrivilegeManager
import btm.m.edgeflow.sidebar.model.CardConfig
import btm.m.edgeflow.sidebar.model.CardConfigManager
import btm.m.edgeflow.ui.MainViewModel
import btm.m.edgeflow.ui.UiState
import btm.m.edgeflow.ui.locale.AppLocale
import btm.m.edgeflow.ui.theme.EdgeFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var privilegeManager: PrivilegeManager

    private var viewModel: MainViewModel? = null

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel?.refreshOverlayPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdgeFlowTheme {
                val vm: MainViewModel = hiltViewModel()
                viewModel = vm
                vm.refreshOverlayPermission()
                val uiState by vm.uiState.collectAsState()

                MainSettingsScreen(
                    uiState = uiState,
                    privilegeManager = privilegeManager,
                    onRequestOverlayPermission = {
                        overlayPermissionLauncher.launch(vm.overlayPermissionIntent())
                    },
                    onToggleService = {
                        if (!uiState.hasOverlayPermission) {
                            overlayPermissionLauncher.launch(vm.overlayPermissionIntent())
                        } else {
                            vm.toggleService()
                        }
                    },
                    onRequestShizuku = { privilegeManager.requestShizukuPermission() },
                    onOpenEditApps = {
                        startActivity(Intent(this, EditAppActivity::class.java))
                    },
                    onRequestNotificationListener = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.refreshOverlayPermission()
        viewModel?.checkNotificationListenerPermission(this)
    }
}

// ─── Main Settings Screen ────────────────────────────────────────────────────

@Composable
private fun MainSettingsScreen(
    uiState: UiState,
    privilegeManager: PrivilegeManager,
    onRequestOverlayPermission: () -> Unit,
    onToggleService: () -> Unit,
    onRequestShizuku: () -> Unit,
    onOpenEditApps: () -> Unit,
    onRequestNotificationListener: () -> Unit
) {
    val rootAvailable by privilegeManager.rootAvailable.collectAsState()
    val shizukuAvailable by privilegeManager.shizukuAvailable.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val s = AppLocale.strings()

    // ── DataStore flows ────────────────────────────────────────────────────
    val blurRadius by UserPreferences.blurRadiusFlow(context)
        .collectAsState(initial = 35f)
    val panelAlpha by UserPreferences.panelAlphaFlow(context)
        .collectAsState(initial = 0.65f)
    val headerImageUri by UserPreferences.headerImageUriFlow(context)
        .collectAsState(initial = null)
    val bgImageUri by UserPreferences.bgImageUriFlow(context)
        .collectAsState(initial = null)
    val bgImageAlpha by UserPreferences.bgImageAlphaFlow(context)
        .collectAsState(initial = 0.5f)
    val bgImageBlur by UserPreferences.bgImageBlurFlow(context)
        .collectAsState(initial = 15f)
    val widthRatio by UserPreferences.sidebarWidthRatioFlow(context)
        .collectAsState(initial = 0.70f)

    // Header image picker
    val headerImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            scope.launch { UserPreferences.setHeaderImageUri(context, uri.toString()) }
        }
    }

    // Background image picker
    val bgImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            scope.launch { UserPreferences.setBgImageUri(context, uri.toString()) }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Title ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.edgeflow,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = s.globalEdgeSidebar,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    context.startActivity(Intent(context, btm.m.edgeflow.AboutActivity::class.java))
                }) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = s.about,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ════════════════════════════════════════════════════════════════
            // SECTION: Permission Status
            // ════════════════════════════════════════════════════════════════

            SectionLabel(s.permissionsService)

            // Privilege Status Card
            PrivilegeStatusCard(
                rootAvailable = rootAvailable,
                shizukuAvailable = shizukuAvailable,
                onRequestShizuku = onRequestShizuku
            )

            // Notification Listener Permission Card
            if (!uiState.hasNotificationListenerPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    s.mediaNotificationRequired,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    s.mediaNotificationDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestNotificationListener,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(s.grantAccess)
                        }
                    }
                }
            } else {
                // Show granted status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            s.mediaNotificationGranted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Overlay Permission Card
            if (!uiState.hasOverlayPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            s.overlayPermissionRequired,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            s.overlayPermissionDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRequestOverlayPermission) {
                            Text(s.grantPermission)
                        }
                    }
                }
            }

            // Service Toggle
            val isRunning = uiState.isServiceRunning
            Button(
                onClick = onToggleService,
                enabled = uiState.hasOverlayPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isRunning) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (isRunning) s.stopEdgeListener else s.startEdgeListener)
            }

            // Edit Frequent Apps
            SettingsCard(
                icon = Icons.Default.Edit,
                title = s.editFrequentApps,
                subtitle = s.editFrequentAppsDesc,
                onClick = onOpenEditApps
            )

            // Trigger & Interaction Settings
            SettingsCard(
                icon = Icons.Default.Settings,
                title = s.triggerInteraction,
                subtitle = s.triggerInteractionDesc,
                onClick = {
                    context.startActivity(Intent(context, btm.m.edgeflow.TriggerSettingsActivity::class.java))
                }
            )

            // Custom Quick Links
            SettingsCard(
                icon = Icons.Default.Edit,
                title = s.customQuickLinks,
                subtitle = s.customQuickLinksDesc,
                onClick = {
                    context.startActivity(Intent(context, btm.m.edgeflow.CustomLinksActivity::class.java))
                }
            )

            // Card Layout Manager
            CardLayoutManager()

            Spacer(modifier = Modifier.height(4.dp))

            // ════════════════════════════════════════════════════════════════
            // SECTION: Global Panel
            // ════════════════════════════════════════════════════════════════

            SectionLabel(s.globalPanel)

            // Global Blur Radius
            SettingSlider(
                title = s.globalBlurRadius,
                value = blurRadius,
                valueRange = 0f..100f,
                label = "${blurRadius.toInt()}px",
                onValueChange = { newValue ->
                    scope.launch { UserPreferences.setBlurRadius(context, newValue) }
                },
                description = "0 — off · 100 — maximum blur"
            )

            // Global Panel Alpha
            SettingSlider(
                title = s.panelTransparency,
                value = panelAlpha,
                valueRange = 0.1f..1f,
                label = "${(panelAlpha * 100).toInt()}%",
                onValueChange = { newValue ->
                    scope.launch { UserPreferences.setPanelAlpha(context, newValue) }
                },
                description = "10% — nearly invisible · 100% — fully opaque"
            )

            // Sidebar Width
            SettingSlider(
                title = s.sidebarWidth,
                value = widthRatio,
                valueRange = 0.5f..1.0f,
                label = "${(widthRatio * 100).toInt()}%",
                onValueChange = { newValue ->
                    scope.launch { UserPreferences.setSidebarWidthRatio(context, newValue) }
                },
                description = "50% — narrow · 100% — full screen"
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ════════════════════════════════════════════════════════════════
            // SECTION: Header Image Customization
            // ════════════════════════════════════════════════════════════════

            SectionLabel(s.headerImage)

            // Header image picker
            SettingsCard(
                icon = Icons.Default.Image,
                title = s.customHeaderImage,
                subtitle = if (headerImageUri.isNullOrBlank()) s.tapToSelectImage
                           else s.imageSet,
                onClick = { headerImagePicker.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ════════════════════════════════════════════════════════════════
            // SECTION: Background Image
            // ════════════════════════════════════════════════════════════════

            SectionLabel(s.backgroundImage)

            // Background image picker
            SettingsCard(
                icon = Icons.Default.Image,
                title = s.backgroundImage,
                subtitle = if (bgImageUri.isNullOrBlank()) s.tapToSelectBg
                           else s.imageSet,
                onClick = { bgImagePicker.launch("image/*") }
            )

            // Clear background button (only when image is set)
            if (!bgImageUri.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { UserPreferences.setBgImageUri(context, null) }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { UserPreferences.setBgImageUri(context, null) }
                            }
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            s.clearBackground,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!bgImageUri.isNullOrBlank()) {
                // Background Image Alpha
                SettingSlider(
                    title = s.bgOpacity,
                    value = bgImageAlpha,
                    valueRange = 0f..1f,
                    label = "${(bgImageAlpha * 100).toInt()}%",
                    onValueChange = { newValue ->
                        scope.launch { UserPreferences.setBgImageAlpha(context, newValue) }
                    },
                    description = "0% — invisible · 100% — fully visible"
                )

                // Background Image Blur
                SettingSlider(
                    title = s.bgBlur,
                    value = bgImageBlur,
                    valueRange = 0f..50f,
                    label = "${bgImageBlur.toInt()}px",
                    onValueChange = { newValue ->
                        scope.launch { UserPreferences.setBgImageBlur(context, newValue) }
                    },
                    description = "0 — sharp · 50 — very blurry"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ════════════════════════════════════════════════════════════════
            // SECTION: Language Switcher
            // ════════════════════════════════════════════════════════════════

            LanguageSwitcher()

            Spacer(modifier = Modifier.height(16.dp))

            // ── Status text ───────────────────────────────────────────────
            Text(
                text = if (isRunning) {
                    s.serviceRunning
                } else {
                    s.serviceStopped
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Section Label ───────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

// ─── Settings Card ───────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Setting Slider ──────────────────────────────────────────────────────────

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    onValueChange: (Float) -> Unit,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Privilege Status Card ───────────────────────────────────────────────────

@Composable
private fun PrivilegeStatusCard(
    rootAvailable: Boolean,
    shizukuAvailable: Boolean,
    onRequestShizuku: () -> Unit
) {
    val s = AppLocale.strings()
    val (statusText, statusColor, detailText) = when {
        rootAvailable -> Triple(
            s.rootActive,
            MaterialTheme.colorScheme.primary,
            s.rootActiveDesc
        )
        shizukuAvailable -> Triple(
            s.shizukuConnected,
            MaterialTheme.colorScheme.tertiary,
            s.shizukuDesc
        )
        else -> Triple(
            s.noPrivilege,
            MaterialTheme.colorScheme.error,
            s.noPrivilegeDesc
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security, null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor
                    )
                    Text(
                        detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!rootAvailable && !shizukuAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestShizuku,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.requestShizuku)
                }
            }
        }
    }
}

// ─── Language Switcher ──────────────────────────────────────────────────────

@Composable
private fun LanguageSwitcher() {
    val expanded = remember { mutableStateOf(false) }
    val currentLang = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = true }
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Language / 语言", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (currentLang == "zh") "中文" else "English",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                DropdownMenuItem(
                    text = { Text("English") },
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        expanded.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("中文") },
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
                        expanded.value = false
                    }
                )
            }
        }
    }
}

// ─── Card Layout Manager ────────────────────────────────────────────────────

@Composable
private fun CardLayoutManager() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = AppLocale.strings()
    val cardConfigs by CardConfigManager.configFlow(context)
        .collectAsState(initial = CardConfigManager.DEFAULT_CONFIGS)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                s.cardLayout,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                s.cardLayoutDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card list with up/down buttons and visibility toggle
            // Use a Switch import
            val switchColors = androidx.compose.material3.SwitchDefaults.colors()

            cardConfigs.forEachIndexed { index, config ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Card name
                    Text(
                        config.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (config.isVisible) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    // Visibility toggle
                    androidx.compose.material3.Switch(
                        checked = config.isVisible,
                        onCheckedChange = {
                            scope.launch {
                                CardConfigManager.toggleVisibility(context, config.id)
                            }
                        },
                        colors = switchColors
                    )

                    // Up arrow
                    IconButton(
                        onClick = {
                            scope.launch { CardConfigManager.moveCard(context, config.id, -1) }
                        },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = if (index > 0) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Down arrow
                    IconButton(
                        onClick = {
                            scope.launch { CardConfigManager.moveCard(context, config.id, 1) }
                        },
                        enabled = index < cardConfigs.size - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = if (index < cardConfigs.size - 1) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reset button
            Text(
                s.resetToDefault,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        scope.launch { CardConfigManager.resetToDefault(context) }
                    }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

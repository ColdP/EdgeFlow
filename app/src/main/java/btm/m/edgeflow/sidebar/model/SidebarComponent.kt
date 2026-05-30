package btm.m.edgeflow.sidebar.model

import android.graphics.Bitmap

/**
 * Sealed hierarchy representing every card type that can appear in the sidebar.
 * Stored in Room (Module 3); for now we use in-memory mock data.
 */
sealed interface SidebarComponent {
    /** Unique identifier for ordering & diffing inside LazyColumn. */
    val id: String
}

/**
 * A quick-launch tile for an installed application.
 */
data class AppShortcut(
    override val id: String,
    val packageName: String,
    val label: String,
    /** URI or resource name for the app icon. In production this resolves from PackageManager. */
    val iconUri: String = ""
) : SidebarComponent

/**
 * A toggle for a system-level setting (Wi-Fi, Bluetooth, DND, etc.).
 */
data class SystemControl(
    override val id: String,
    val type: ControlType,
    val label: String,
    val enabled: Boolean = false
) : SidebarComponent

enum class ControlType {
    WIFI, BLUETOOTH, DO_NOT_DISTURB, FLASHLIGHT, ROTATION, AIRPLANE,
    MOBILE_DATA, GPS, NOTIFICATION
}

/**
 * A media playback control card for the sidebar.
 */
data class MediaCard(
    override val id: String,
    val title: String = "No media playing",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val albumArt: Bitmap? = null
) : SidebarComponent

/**
 * Maps a [ControlType] to its corresponding shell command for privileged execution.
 *
 * @param enabled The desired toggle state.
 * @return The shell command string, or `null` if no shell command is available for this type.
 */
fun ControlType.toShellCommand(enabled: Boolean): String? = when (this) {
    ControlType.WIFI -> "svc wifi ${if (enabled) "enable" else "disable"}"
    ControlType.BLUETOOTH -> "svc bluetooth ${if (enabled) "enable" else "disable"}"
    ControlType.DO_NOT_DISTURB -> {
        if (enabled) "settings put global zen_mode 1"
        else "settings put global zen_mode 0"
    }
    ControlType.FLASHLIGHT -> {
        // cmd camera_service doesn't exist on all devices; settings-based approach is more reliable
        "cmd statusbar ${if (enabled) "expand-notifications" else "collapse"}"
    }
    ControlType.ROTATION -> {
        if (enabled) "settings put system accelerometer_rotation 1"
        else "settings put system accelerometer_rotation 0"
    }
    ControlType.AIRPLANE -> {
        if (enabled) "settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"
        else "settings put global airplane_mode_on 0 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false"
    }
    ControlType.MOBILE_DATA -> "svc data ${if (enabled) "enable" else "disable"}"
    ControlType.GPS -> "settings put secure location_mode ${if (enabled) "3" else "0"}"
    ControlType.NOTIFICATION -> "cmd statusbar ${if (enabled) "expand-notifications" else "collapse"}"
}

/**
 * A read-only information tile (e.g. battery level, storage usage).
 */
data class InfoCard(
    override val id: String,
    val title: String,
    val value: String,
    val unit: String = ""
) : SidebarComponent

/**
 * A quick-pay shortcut button (WeChat/Alipay scan/pay).
 */
data class ShortcutCard(
    override val id: String = "shortcut_card",
    val shortcuts: List<PayShortcut> = emptyList()
) : SidebarComponent

data class PayShortcut(
    val label: String,
    val iconKey: String,
    val actionType: String,  // "scheme" or "shell"
    val actionTarget: String  // URI or shell command
)

/**
 * A grouped section header that visually separates card groups.
 */
data class SectionHeader(
    override val id: String,
    val title: String
) : SidebarComponent

/** Default card order IDs. Header is always fixed at index 0 and not included here. */
object CardOrderDefaults {
    const val ID_APPS = "apps"
    const val ID_SHORTCUT = "shortcut"
    const val ID_MEDIA = "media"
    const val ID_SYSTEM = "system"
    const val ID_CLIPBOARD = "clipboard"

    val DEFAULT_ORDER = listOf(ID_APPS, ID_SHORTCUT, ID_MEDIA, ID_SYSTEM, ID_CLIPBOARD)
}

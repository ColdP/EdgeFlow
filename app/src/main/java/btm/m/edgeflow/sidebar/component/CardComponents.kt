package btm.m.edgeflow.sidebar.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import btm.m.edgeflow.sidebar.model.AppShortcut
import btm.m.edgeflow.sidebar.model.ControlType
import btm.m.edgeflow.sidebar.model.InfoCard
import btm.m.edgeflow.sidebar.model.MediaCard
import btm.m.edgeflow.sidebar.model.SectionHeader
import btm.m.edgeflow.sidebar.model.SystemControl

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
fun SectionHeaderCard(section: SectionHeader) {
    Text(
        text = section.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    )
}

// ─── App Shortcut Card ───────────────────────────────────────────────────────

@Composable
fun AppCard(
    shortcut: AppShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap: ImageBitmap? = remember(shortcut.packageName) {
        loadAppIcon(context.packageManager, shortcut.packageName)
    }

    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = iconBitmap,
                        contentDescription = shortcut.label,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = shortcut.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun loadAppIcon(pm: android.content.pm.PackageManager, packageName: String): ImageBitmap? {
    return try {
        val drawable: Drawable = pm.getApplicationIcon(packageName)
        drawable.toBitmap(128, 128).asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

// ─── System Control Card ─────────────────────────────────────────────────────

@Composable
fun SystemControlCard(
    control: SystemControl,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        onClick = { onToggle(!control.enabled) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val icon = control.type.toImageVector()
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (control.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = control.label,
                    tint = if (control.enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = control.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = control.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

// ─── Info Card ───────────────────────────────────────────────────────────────

@Composable
fun InfoCardComponent(
    info: InfoCard,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${info.value} ${info.unit}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─── Usage Permission Card ───────────────────────────────────────────────────

@Composable
fun UsagePermissionCard(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        onClick = onGrant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable App Suggestions",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Grant usage access to show your most-used apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun ControlType.toImageVector(): ImageVector = when (this) {
    ControlType.WIFI -> Icons.Default.Wifi
    ControlType.BLUETOOTH -> Icons.Default.Bluetooth
    ControlType.DO_NOT_DISTURB -> Icons.Default.DoNotDisturb
    ControlType.FLASHLIGHT -> Icons.Default.FlashOn
    ControlType.ROTATION -> Icons.Default.ScreenRotation
    ControlType.AIRPLANE -> Icons.Default.AirplanemodeActive
    ControlType.MOBILE_DATA -> Icons.Default.Wifi // fallback
    ControlType.GPS -> Icons.Default.Wifi // fallback
    ControlType.NOTIFICATION -> Icons.Default.DoNotDisturb // fallback
}

package btm.m.edgeflow

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import btm.m.edgeflow.ui.locale.AppLocale
import btm.m.edgeflow.ui.theme.EdgeFlowTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdgeFlowTheme {
                AboutScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = AppLocale.strings()
    val uriHandler = LocalUriHandler.current

    // Dynamic app icon
    val appIcon: ImageBitmap? = remember {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(context.packageName)
            drawable.toBitmap(128, 128).asImageBitmap()
        } catch (_: Exception) { null }
    }

    // Dynamic version info
    val versionInfo: String = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            "$versionName ($versionCode)"
        } catch (_: Exception) { "unknown" }
    }

    // Dynamic system fingerprint — full brand coverage
    val systemFingerprint: String = remember {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val identifier = "$brand $manufacturer"
        val osName = when {
            identifier.contains("xiaomi") || identifier.contains("redmi") || identifier.contains("poco") -> "HyperOS / MIUI"
            identifier.contains("oppo") || identifier.contains("oneplus") -> "ColorOS"
            identifier.contains("vivo") || identifier.contains("iqoo") -> "OriginOS"
            identifier.contains("honor") -> "MagicOS"
            identifier.contains("huawei") -> "HarmonyOS"
            identifier.contains("samsung") -> "One UI"
            identifier.contains("meizu") -> "Flyme"
            identifier.contains("realme") -> "realme UI"
            identifier.contains("nothing") -> "Nothing OS"
            identifier.contains("motorola") || identifier.contains("moto") -> "MyUI / Hello UI"
            identifier.contains("google") || identifier.contains("pixel") -> "Pixel UI"
            identifier.contains("sony") -> "My UX"
            identifier.contains("asus") || identifier.contains("rog") -> "ROG UI / ZenUI"
            identifier.contains("nubia") || identifier.contains("zte") -> "MyOS"
            identifier.contains("lenovo") -> "ZUI"
            identifier.contains("blackshark") -> "JoyUI"
            else -> Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        "$osName • Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.about) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Card 1: App & System Info ─────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // App icon + name + version
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = "App Icon",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            Text(
                                "EF",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(top = 16.dp, start = 16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "EdgeFlow",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                versionInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Developer
                    Text(
                        s.developedBy,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // App description
                    Text(
                        s.appDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // System fingerprint
                    Text(
                        s.appInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        systemFingerprint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Card 2: Developer Links ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        s.links,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    LinkItem(
                        label = s.githubPage,
                        url = "https://github.com/ColdP/EdgeFlow",
                        onClick = { uriHandler.openUri("https://github.com/ColdP/EdgeFlow") }
                    )

                    LinkItem(
                        label = s.officialSite,
                        url = "https://eflow.btm-m.site",
                        onClick = { uriHandler.openUri("https://eflow.btm-m.site") }
                    )

                    LinkItem(
                        label = s.btmMSite,
                        url = "https://btm-m.site",
                        onClick = { uriHandler.openUri("https://btm-m.site") }
                    )

                    LinkItem(
                        label = s.btmMBlog,
                        url = "https://btm-m.live",
                        onClick = { uriHandler.openUri("https://btm-m.live") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinkItem(label: String, url: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
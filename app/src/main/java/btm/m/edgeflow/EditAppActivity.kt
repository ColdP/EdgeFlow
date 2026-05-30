package btm.m.edgeflow

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import btm.m.edgeflow.data.dao.SelectedAppDao
import btm.m.edgeflow.data.entity.SelectedApp
import btm.m.edgeflow.ui.theme.EdgeFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Data class representing an installed application.
 */
data class AppShortcutInfo(val packageName: String, val label: String)

/**
 * Loads all installed launchable apps from the system.
 */
fun loadAllInstalledApps(context: Context): List<AppShortcutInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .mapNotNull { resolveInfo ->
            try {
                val pkg = resolveInfo.activityInfo.packageName
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                AppShortcutInfo(packageName = pkg, label = label)
            } catch (_: Exception) { null }
        }
        .sortedBy { it.label.lowercase() }
}

/**
 * Full-screen activity for batch-editing sidebar apps.
 *
 * Displays all installed launchable apps in a 5-column grid.
 * Apps already in Room are pre-checked. User can toggle checkboxes
 * and tap "Save" to bulk-update the Room database.
 */
@AndroidEntryPoint
class EditAppActivity : ComponentActivity() {

    @Inject lateinit var selectedAppDao: SelectedAppDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EdgeFlowTheme {
                val selectedApps by selectedAppDao.getAll().collectAsState(initial = emptyList())
                val selectedPkgs = remember(selectedApps) { selectedApps.map { it.packageName }.toSet() }

                val allApps = remember { listOf<AppShortcutInfo>().toMutableStateList() }
                val coroutineScope = rememberCoroutineScope()

                // Load all installed apps once
                remember {
                    coroutineScope.launch {
                        val pm = this@EditAppActivity.packageManager
                        val apps = withContext(Dispatchers.IO) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                            }
                            pm.queryIntentActivities(intent, 0)
                                .filter { it.activityInfo.packageName != this@EditAppActivity.packageName }
                                .distinctBy { it.activityInfo.packageName }
                                .mapNotNull { resolveInfo ->
                                    try {
                                        val pkg = resolveInfo.activityInfo.packageName
                                        val appInfo = pm.getApplicationInfo(pkg, 0)
                                        val label = pm.getApplicationLabel(appInfo).toString()
                                        AppShortcutInfo(packageName = pkg, label = label)
                                    } catch (_: Exception) { null }
                                }
                                .sortedBy { it.label.lowercase() }
                        }
                        allApps.clear()
                        allApps.addAll(apps)
                    }
                    false
                }

                // Checkbox state
                val checkState = remember { mutableStateMapOf<String, Boolean>() }
                val selPkgs = selectedPkgs
                allApps.forEach { app: AppShortcutInfo ->
                    if (app.packageName !in checkState) {
                        checkState[app.packageName] = app.packageName in selPkgs
                    }
                }

                EditAppScreen(
                    allApps = allApps,
                    checkState = checkState,
                    onToggle = { pkg, checked ->
                        checkState[pkg] = checked
                    },
                    onSave = {
                        coroutineScope.launch {
                            val appsList: List<AppShortcutInfo> = allApps.toList()
                            val toRemove: List<String> = appsList.filter { a -> checkState[a.packageName] != true }.map { a -> a.packageName }
                            val toInsert: List<AppShortcutInfo> = appsList.filter { a -> checkState[a.packageName] == true }

                            toRemove.forEach { pkg -> selectedAppDao.deleteByPackageName(pkg) }
                            val entities: List<SelectedApp> = toInsert.mapIndexed { idx: Int, app: AppShortcutInfo ->
                                SelectedApp(packageName = app.packageName, label = app.label, sortOrder = idx)
                            }
                            selectedAppDao.insertAll(entities)
                            finish()
                        }
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAppScreen(
    allApps: List<AppShortcutInfo>,
    checkState: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Sidebar Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSave) {
                Icon(Icons.Default.Save, "Save")
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section header
            item(span = { GridItemSpan(5) }) {
                Text(
                    "Check apps to show in sidebar. Long-press to see details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            items(allApps, key = { it.packageName }) { app ->
                val isChecked = checkState[app.packageName] == true
                EditGridItem(
                    app = app,
                    isChecked = isChecked,
                    onClick = { onToggle(app.packageName, !isChecked) }
                )
            }
        }
    }
}

@Composable
private fun EditGridItem(
    app: AppShortcutInfo,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    val pm = LocalContext.current.packageManager
    val iconBitmap: ImageBitmap? = remember(app.packageName) {
        try {
            val drawable: Drawable = pm.getApplicationIcon(app.packageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) { null }
    }

    val alpha = if (isChecked) 1f else 0.5f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(38.dp))
            } else {
                Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
            // Checkmark overlay
            if (isChecked) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
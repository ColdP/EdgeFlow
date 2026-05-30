package btm.m.edgeflow

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import btm.m.edgeflow.data.dao.SelectedAppDao
import btm.m.edgeflow.data.entity.SelectedApp
import btm.m.edgeflow.engine.ActionExecutor
import btm.m.edgeflow.engine.PrivilegeManager
import btm.m.edgeflow.sidebar.SidebarViewModel
import btm.m.edgeflow.sidebar.model.AppShortcut
import btm.m.edgeflow.ui.locale.AppLocale
import btm.m.edgeflow.ui.theme.EdgeFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Full-screen Activity for picking apps to add to the sidebar.
 *
 * Displays all installed launchable apps in a 4-column grid.
 * Tapping an app inserts it into Room and finishes.
 */
@AndroidEntryPoint
class AppPickerActivity : ComponentActivity() {

    @Inject lateinit var selectedAppDao: SelectedAppDao
    @Inject lateinit var customLinkDao: btm.m.edgeflow.data.dao.CustomLinkDao
    @Inject lateinit var actionExecutor: ActionExecutor
    @Inject lateinit var privilegeManager: PrivilegeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create ViewModel manually with Hilt-injected deps
        val viewModel = SidebarViewModel(selectedAppDao, customLinkDao, actionExecutor, privilegeManager)
        viewModel.loadAllInstalledApps(this)

        val alreadySelected = mutableSetOf<String>()
        CoroutineScope(Dispatchers.IO).launch {
            selectedAppDao.getAll().collect { apps ->
                alreadySelected.clear()
                alreadySelected.addAll(apps.map { it.packageName })
            }
        }

        setContent {
            EdgeFlowTheme {
                val allApps by viewModel.allInstalledApps.collectAsState()
                val selectedApps by viewModel.selectedApps.collectAsState()
                val selectedPkgs = remember(selectedApps) { selectedApps.map { it.packageName }.toSet() }

                AppPickerScreen(
                    apps = allApps,
                    selectedPackages = selectedPkgs,
                    onAppPicked = { app ->
                        viewModel.addApp(app.packageName, app.label)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

// ─── App Picker Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerScreen(
    apps: List<AppShortcut>,
    selectedPackages: Set<String>,
    onAppPicked: (AppShortcut) -> Unit,
    onBack: () -> Unit
) {
    val s = AppLocale.strings()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.addApp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps, key = { it.id }) { app ->
                val isAlreadyAdded = app.packageName in selectedPackages
                PickerGridItem(
                    app = app,
                    isAlreadyAdded = isAlreadyAdded,
                    onClick = { if (!isAlreadyAdded) onAppPicked(app) }
                )
            }
        }
    }
}

@Composable
private fun PickerGridItem(
    app: AppShortcut,
    isAlreadyAdded: Boolean,
    onClick: () -> Unit
) {
    val pm = LocalContext.current.packageManager
    val iconBitmap: ImageBitmap? = remember(app.packageName) {
        try {
            val drawable: Drawable = pm.getApplicationIcon(app.packageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) { null }
    }

    val alpha = if (isAlreadyAdded) 0.4f else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isAlreadyAdded, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    modifier = Modifier.size(28.dp)
                )
            }
            if (isAlreadyAdded) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Added",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
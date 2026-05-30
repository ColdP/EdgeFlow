package btm.m.edgeflow.sidebar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import btm.m.edgeflow.data.entity.CustomLink
import btm.m.edgeflow.sidebar.model.AppShortcut
import btm.m.edgeflow.sidebar.model.CardConfig
import btm.m.edgeflow.sidebar.model.ControlType
import btm.m.edgeflow.sidebar.model.MediaCard
import btm.m.edgeflow.sidebar.model.SectionHeader
import btm.m.edgeflow.sidebar.model.SidebarComponent
import btm.m.edgeflow.sidebar.model.SystemControl

/**
 * Full-screen sidebar overlay.
 *
 * ## Layout Architecture
 * ```
 * ┌──────────────────────────────────────────────┐
 * │                                              │
 * │  ┌──────────────┐  ┌────────────────────────┐│
 * │  │              │  │                        ││
 * │  │    PANEL     │  │   TRANSPARENT TAP ZONE ││
 * │  │  (70% width) │  │   (30% width)          ││
 * │  │              │  │   tap → dismiss        ││
 * │  │  Grid + Cards│  │                        ││
 * │  │              │  │                        ││
 * │  └──────────────┘  └────────────────────────┘│
 * └──────────────────────────────────────────────┘
 *         ↑ WindowManager is MATCH_PARENT, fully transparent, with FLAG_BLUR_BEHIND
 * ```
 *
 * ## Animation Model
 * A single `progress: Animatable<Float>` (0f=hidden, 1f=visible) drives ALL visual state.
 * Drag-to-dismiss and auto-open both modify this same variable — no flash-back possible.
 */
@Composable
fun SidebarScreen(
    config: SidebarConfig,
    components: List<SidebarComponent>,
    cardConfigs: List<CardConfig> = emptyList(),
    customLinks: List<CustomLink> = emptyList(),
    headerImageUri: String?,
    dismissTrigger: Int = 0,
    onRemoveOverlay: () -> Unit,
    onAppClick: (AppShortcut) -> Unit,
    onAppLongPress: (AppShortcut) -> Unit,
    onSystemToggled: (SystemControl, Boolean) -> Unit,
    onAddApp: () -> Unit,
    onRefreshSystemState: () -> Unit,
    onBlurRadiusChanged: (Int) -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onShortcutClick: (btm.m.edgeflow.sidebar.model.PayShortcut) -> Unit = {},
    onCustomLinkClick: (String) -> Unit = {}
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val panelWidthDp = (screenWidthDp * config.panelWidthFraction).coerceAtMost(420.dp)

    val maxBlur = config.blurRadius

    // ── Unified animation progress ──────────────────────────────────────
    val progress = remember { Animatable(0f) }

    // Auto-open: animate from 0 → 1 on first composition
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    // Drive blur radius from progress (0 → maxBlur)
    LaunchedEffect(progress.value) {
        onBlurRadiusChanged((progress.value * maxBlur).toInt())
    }

    // Refresh system state on mount
    LaunchedEffect(Unit) { onRefreshSystemState() }

    // React to back key dismiss trigger
    LaunchedEffect(dismissTrigger) {
        if (dismissTrigger > 0) {
            progress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
            onRemoveOverlay()
        }
    }

    // Coroutine scope for launching animations from non-suspend callbacks
    val scope = rememberCoroutineScope()

    // Remove dialog state
    val appToRemove = remember { androidx.compose.runtime.mutableStateOf<AppShortcut?>(null) }

    appToRemove.value?.let { app ->
        AlertDialog(
            onDismissRequest = { appToRemove.value = null },
            title = { Text("Remove ${app.label}?") },
            text = { Text("This will remove ${app.label} from the sidebar.") },
            confirmButton = {
                TextButton(onClick = {
                    onAppLongPress(app)
                    appToRemove.value = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { appToRemove.value = null }) { Text("Cancel") }
            }
        )
    }

    // ── Root container: full screen ──────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // ═══════════════════════════════════════════════════════════════════
        // Transparent tap zone (right 30%) — tap to dismiss
        // ═══════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    scope.launch {
                        progress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        onRemoveOverlay()
                    }
                }
        )

        // ═══════════════════════════════════════════════════════════════════
        // Panel (left 70%) — slides in/out driven by progress
        // ═══════════════════════════════════════════════════════════════════
        val panelShape = remember { RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp) }

        // Calculate pixel offset for the animation
        val density = LocalConfiguration.current.densityDpi / 160f
        val panelWidthPx = with(panelWidthDp) { this.value * density }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(panelWidthDp)
                .graphicsLayer {
                    // Slide from left: progress=0 → offscreen, progress=1 → at rest
                    translationX = (progress.value - 1f) * panelWidthPx
                    // Subtle scale-down during drag
                    val scale = 0.96f + 0.04f * progress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = progress.value
                }
                .clip(panelShape)
                .then(
                    // Only apply status bar padding when NOT 100% width (immersive)
                    if (config.panelWidthFraction < 1.0f) Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    else Modifier
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount < 0f || progress.value < 1f) {
                                scope.launch {
                                    val newValue = (progress.value + dragAmount / panelWidthPx).coerceIn(0f, 1f)
                                    progress.snapTo(newValue)
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (progress.value < 0.55f) {
                                    progress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    onRemoveOverlay()
                                } else {
                                    progress.animateTo(1f, spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                progress.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        }
                    )
                }
        ) {
            // ── Background Image Layer (bottom-most) ─────────────────────
            val hasBackgroundImage = config.backgroundUri.isNotBlank()
            if (hasBackgroundImage) {
                AsyncImage(
                    model = config.backgroundUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                        .blur(radius = config.bgImageBlur.dp)
                        .graphicsLayer {
                            alpha = config.bgImageAlpha
                        }
                )
            }

            // ── Panel surface overlay (semi-transparent on top of bg) ────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = config.maskAlpha),
                        shape = panelShape
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Header Image ─────────────────────────────────────
                    HeaderImage(
                        imageUri = headerImageUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    // ── App Grid + System Controls + Media Card ──────────
                    AppGrid(
                        components = components,
                        cardConfigs = cardConfigs,
                        onAppClick = onAppClick,
                        onAppLongClick = { appToRemove.value = it },
                        onSystemToggled = onSystemToggled,
                        onAddApp = onAddApp,
                        onMediaPlayPause = onMediaPlayPause,
                        onMediaNext = onMediaNext,
                        onMediaPrevious = onMediaPrevious,
                        onShortcutClick = onShortcutClick,
                        customLinks = customLinks,
                        onCustomLinkClick = onCustomLinkClick,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )

                    // ── Footer ───────────────────────────────────────────
                    Text(
                        text = "Swipe left or tap right to close",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 20.dp, top = 6.dp)
                    )
                }
            }
        }
    }
}

// ─── Header Image ────────────────────────────────────────────────────────────

@Composable
private fun HeaderImage(imageUri: String?, modifier: Modifier = Modifier) {
    val shape = remember { RoundedCornerShape(24.dp) }
    Box(modifier = modifier.clip(shape), contentAlignment = Alignment.Center) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Header",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            )
                        ),
                        shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Landscape, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Set a custom header image",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                )
            }
        }
    }
}

// ─── App Grid ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(
    components: List<SidebarComponent>,
    cardConfigs: List<CardConfig> = emptyList(),
    onAppClick: (AppShortcut) -> Unit,
    onAppLongClick: (AppShortcut) -> Unit,
    onSystemToggled: (SystemControl, Boolean) -> Unit,
    onAddApp: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrevious: () -> Unit,
    onShortcutClick: (btm.m.edgeflow.sidebar.model.PayShortcut) -> Unit = {},
    customLinks: List<CustomLink> = emptyList(),
    onCustomLinkClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Filter components by cardConfigs visibility — only render visible card groups
    val visibleCardIds = cardConfigs.filter { it.isVisible }.map { it.id }.toSet()

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        components.forEach { component ->
            // Skip cards whose group is hidden in CardConfig
            val shouldShow = when (component) {
                is SectionHeader -> when {
                    component.id.contains("apps") -> "apps" in visibleCardIds
                    component.id.contains("shortcut") -> "shortcut" in visibleCardIds
                    component.id.contains("media") -> "media" in visibleCardIds
                    component.id.contains("system") -> "system" in visibleCardIds
                    else -> true
                }
                is AppShortcut -> "apps" in visibleCardIds
                is SystemControl -> "system" in visibleCardIds
                is MediaCard -> "media" in visibleCardIds
                is btm.m.edgeflow.sidebar.model.ShortcutCard -> "shortcut" in visibleCardIds
                is btm.m.edgeflow.sidebar.model.InfoCard -> true
            }
            if (!shouldShow) return@forEach

            when (component) {
                is SectionHeader -> item(span = { GridItemSpan(5) }, key = component.id) {
                    SectionHeaderRow(component)
                }
                is AppShortcut -> item(key = component.id) {
                    AppGridItem(
                        component,
                        onClick = { onAppClick(component) },
                        onLongClick = { onAppLongClick(component) }
                    )
                }
                is SystemControl -> item(key = component.id) {
                    SystemControlGridItem(
                        control = component,
                        onClick = { onSystemToggled(component, !component.enabled) }
                    )
                }
                is MediaCard -> item(span = { GridItemSpan(5) }, key = component.id) {
                    MediaCardFull(
                        media = component,
                        onPlayPause = onMediaPlayPause,
                        onNext = onMediaNext,
                        onPrevious = onMediaPrevious
                    )
                }
                is btm.m.edgeflow.sidebar.model.ShortcutCard -> item(span = { GridItemSpan(5) }, key = component.id) {
                    ShortcutGrid(shortcuts = component.shortcuts, onShortcutClick = onShortcutClick)
                }
                is btm.m.edgeflow.sidebar.model.InfoCard -> item(span = { GridItemSpan(5) }, key = component.id) {
                    InfoRow(component)
                }
            }
        }
        // Custom Links section
        if (customLinks.isNotEmpty()) {
            item(span = { GridItemSpan(5) }, key = "custom_links_header") {
                SectionHeaderRow(SectionHeader(id = "header_custom", title = "Custom Links"))
            }
            items(customLinks.size, key = { customLinks[it].id }) { index ->
                val link = customLinks[index]
                CustomLinkItem(link = link, onClick = { onCustomLinkClick(link.url) })
            }
        }
        item(key = "add_btn") { AddAppButton(onClick = onAddApp) }
    }
}

// ─── Section Header Row ──────────────────────────────────────────────────────

@Composable
private fun SectionHeaderRow(section: SectionHeader) {
    Text(
        section.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

// ─── App Grid Item ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(app: AppShortcut, onClick: () -> Unit, onLongClick: () -> Unit) {
    val pm = LocalContext.current.packageManager
    val iconBitmap: ImageBitmap? = remember(app.packageName) {
        try {
            pm.getApplicationIcon(app.packageName).toBitmap(128, 128).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(iconBitmap, app.label, modifier = Modifier.size(44.dp))
            } else {
                Icon(
                    Icons.Default.Apps, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Add Button ──────────────────────────────────────────────────────────────

@Composable
private fun AddAppButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add, "Add",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Add",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── System Control Grid Item (iOS Control Center style) ─────────────────────

@Composable
private fun SystemControlGridItem(
    control: SystemControl,
    onClick: () -> Unit
) {
    val icon = control.type.toImageVector()
    val bgColor = if (control.enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    }
    val iconColor = if (control.enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, control.label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            control.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Media Card (full width with transport controls) ─────────────────────────

@Composable
private fun MediaCardFull(
    media: MediaCard,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val isNoMedia = media.title == "No media playing" || media.title.isBlank()
    val albumArtBitmap: ImageBitmap? = remember(media.albumArt) {
        media.albumArt?.let { bmp ->
            try {
                // Scale to consistent size for rendering
                val scaled = Bitmap.createScaledBitmap(bmp, 128, 128, true)
                scaled.asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Album art / placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isNoMedia && albumArtBitmap == null)
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtBitmap != null) {
                    Image(
                        bitmap = albumArtBitmap,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote, null,
                        tint = if (isNoMedia) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isNoMedia) "No media playing" else media.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isNoMedia) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isNoMedia && media.artist.isNotBlank()) {
                    Text(
                        media.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // Transport controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Default.SkipPrevious, "Previous",
                    tint = if (isNoMedia) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                           else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isNoMedia) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    enabled = !isNoMedia,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (media.isPlaying) "Pause" else "Play",
                        tint = if (isNoMedia) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                               else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.SkipNext, "Next",
                    tint = if (isNoMedia) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                           else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─── Shortcut Grid (Quick Actions) ──────────────────────────────────────────

@Composable
private fun ShortcutGrid(
    shortcuts: List<btm.m.edgeflow.sidebar.model.PayShortcut>,
    onShortcutClick: (btm.m.edgeflow.sidebar.model.PayShortcut) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        shortcuts.forEach { shortcut ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onShortcutClick(shortcut) }
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                val icon = when (shortcut.iconKey) {
                    "wechat_scan", "wechat_pay" -> Icons.Default.Apps
                    "alipay_scan", "alipay_pay" -> Icons.Default.CellTower
                    else -> Icons.Default.Apps
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, shortcut.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    shortcut.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Info Row ────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(info: btm.m.edgeflow.sidebar.model.InfoCard) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                info.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${info.value} ${info.unit}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Custom Link Item ────────────────────────────────────────────────────────

@Composable
private fun CustomLinkItem(link: CustomLink, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Landscape, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                link.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                link.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── ControlType to ImageVector ───────────────────────────────────────────────

@Composable
private fun ControlType.toImageVector(): ImageVector = when (this) {
    ControlType.WIFI -> Icons.Default.Wifi
    ControlType.BLUETOOTH -> Icons.Default.Bluetooth
    ControlType.DO_NOT_DISTURB -> Icons.Default.DoNotDisturb
    ControlType.FLASHLIGHT -> Icons.Default.FlashOn
    ControlType.ROTATION -> Icons.Default.ScreenRotation
    ControlType.AIRPLANE -> Icons.Default.AirplanemodeActive
    ControlType.MOBILE_DATA -> Icons.Default.CellTower
    ControlType.GPS -> Icons.Default.GpsFixed
    ControlType.NOTIFICATION -> Icons.Default.Notifications
}
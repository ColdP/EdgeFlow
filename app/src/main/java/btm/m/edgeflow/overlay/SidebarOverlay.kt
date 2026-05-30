package btm.m.edgeflow.overlay

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Legacy full-screen overlay composable for the sidebar panel.
 * Uses native [Modifier.blur] (Android 12+) instead of haze.
 */
@Composable
fun SidebarOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val panelWidthDp = (screenWidthDp * 0.82f).coerceAtMost(380.dp)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Scrim (dimmed background) ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            )

            // ── Sidebar Panel ────────────────────────────────────────────
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetX = { -it }
                ),
                exit = slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    targetOffsetX = { -it }
                ),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                val panelShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)

                Column(
                    modifier = Modifier
                        .width(panelWidthDp)
                        .fillMaxHeight()
                        .clip(panelShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "EdgeFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Swipe to launch · Tap to act",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    PlaceholderCardList()

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Tap outside to close",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderCardList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Card ${index + 1} – placeholder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
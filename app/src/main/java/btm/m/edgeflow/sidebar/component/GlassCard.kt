package btm.m.edgeflow.sidebar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Pure Material Design 3 card container.
 *
 * Uses [MaterialTheme.colorScheme.surfaceContainer] with alpha to let the
 * system-level blur (FLAG_BLUR_BEHIND) show through. No border, no gradient —
 * just a clean MD3 surface with standard ripple feedback.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = remember { RoundedCornerShape(24.dp) }

    var cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
            shape = shape
        )

    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = cardModifier.padding(16.dp),
        content = content
    )
}
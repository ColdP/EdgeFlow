package btm.m.edgeflow.sidebar.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A compact badge displaying the current privilege environment status.
 *
 * Uses MD3 surfaceContainer background and onSurfaceVariant text.
 * Tapping triggers [onRequestShizuku] to initiate Shizuku authorization.
 */
@Composable
fun PrivilegeStatusBadge(
    shizukuAvailable: Boolean,
    rootAvailable: Boolean,
    onRequestShizuku: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        shizukuAvailable -> Color(0xFF4CAF50)
        rootAvailable -> Color(0xFFFFB300)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val label = when {
        shizukuAvailable -> "Shizuku Connected"
        rootAvailable -> "Root Available"
        else -> "No Privilege"
    }

    val animatedDotColor by animateColorAsState(
        targetValue = dotColor,
        animationSpec = tween(durationMillis = 400),
        label = "dotColor"
    )

    val badgeShape = remember { RoundedCornerShape(50) }

    Row(
        modifier = modifier
            .clip(badgeShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                shape = badgeShape
            )
            .clickable(onClick = onRequestShizuku)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(animatedDotColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
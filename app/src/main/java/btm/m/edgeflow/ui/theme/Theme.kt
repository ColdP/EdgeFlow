package btm.m.edgeflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80DEEA),
    secondary = Color(0xFFB39DDB),
    tertiary = Color(0xFFFFAB91),
    surface = Color(0xFF121212),
    surfaceDim = Color(0xFF121212),
    surfaceContainer = Color(0xFF1E1E2E),
    surfaceContainerHigh = Color(0xFF2A2A3A),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    secondary = Color(0xFF7E57C2),
    tertiary = Color(0xFFFF7043),
    surface = Color(0xFFF8F9FA),
    surfaceDim = Color(0xFFF0F1F3),
    surfaceContainer = Color(0xFFE8E9ED),
    surfaceContainerHigh = Color(0xFFDFE0E5),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun EdgeFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ (API 31): use Material You dynamic color from wallpaper
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
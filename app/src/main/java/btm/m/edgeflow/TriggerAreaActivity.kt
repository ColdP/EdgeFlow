package btm.m.edgeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import btm.m.edgeflow.data.UserPreferences
import btm.m.edgeflow.ui.locale.AppLocale
import btm.m.edgeflow.ui.theme.EdgeFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TriggerAreaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EdgeFlowTheme {
                TriggerAreaScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerAreaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = AppLocale.strings()

    val triggerWidth by UserPreferences.triggerWidthFlow(context).collectAsState(initial = 24f)
    val triggerHeight by UserPreferences.triggerHeightFlow(context).collectAsState(initial = 0.6f)
    val triggerYOffset by UserPreferences.triggerYOffsetFlow(context).collectAsState(initial = 0f)
    val triggerSensitivity by UserPreferences.triggerSensitivityFlow(context).collectAsState(initial = 40f)
    val triggerDebug by UserPreferences.triggerDebugFlow(context).collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.advancedAreaSetup) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.highlightTriggerZone, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = triggerDebug,
                            onCheckedChange = { scope.launch { UserPreferences.setTriggerDebug(context, it) } },
                            colors = SwitchDefaults.colors()
                        )
                    }
                    if (triggerDebug) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.highlightTriggerDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${s.triggerZoneWidth}: ${triggerWidth.toInt()}dp", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = triggerWidth,
                        onValueChange = { scope.launch { UserPreferences.setTriggerWidth(context, it) } },
                        valueRange = 8f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("${s.zoneHeight}: ${(triggerHeight * 100).toInt()}% of screen", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = triggerHeight,
                        onValueChange = { scope.launch { UserPreferences.setTriggerHeight(context, it) } },
                        valueRange = 0.2f..1.0f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("${s.yOffset}: ${(triggerYOffset * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = triggerYOffset,
                        onValueChange = { scope.launch { UserPreferences.setTriggerYOffset(context, it) } },
                        valueRange = -0.4f..0.4f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("${s.swipeSensitivity}: ${triggerSensitivity.toInt()}px", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = triggerSensitivity,
                        onValueChange = { scope.launch { UserPreferences.setTriggerSensitivity(context, it) } },
                        valueRange = 10f..120f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        s.sensitivityDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
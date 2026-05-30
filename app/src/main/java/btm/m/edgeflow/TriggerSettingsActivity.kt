package btm.m.edgeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import btm.m.edgeflow.data.UserPreferences
import btm.m.edgeflow.ui.locale.AppLocale
import btm.m.edgeflow.ui.theme.EdgeFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TriggerSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdgeFlowTheme {
                TriggerSettingsScreen(
                    onBack = { finish() },
                    onOpenArea = { startActivity(Intent(this, TriggerAreaActivity::class.java)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerSettingsScreen(onBack: () -> Unit, onOpenArea: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = AppLocale.strings()

    val method by UserPreferences.triggerMethodFlow(context).collectAsState(initial = 0)
    val vibrate by UserPreferences.triggerVibrateFlow(context).collectAsState(initial = true)
    val position by UserPreferences.triggerPositionFlow(context).collectAsState(initial = 0)
    val widthRatio by UserPreferences.sidebarWidthRatioFlow(context).collectAsState(initial = 0.70f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.triggerInteraction) },
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
                    Text(s.triggerMethod, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    MethodItem(s.directSwipe, 0, method) { scope.launch { UserPreferences.setTriggerMethod(context, 0) } }
                    MethodItem(s.doubleSwipe, 1, method) { scope.launch { UserPreferences.setTriggerMethod(context, 1) } }
                    MethodItem(s.floatingBall, 2, method) { scope.launch { UserPreferences.setTriggerMethod(context, 2) } }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(s.edgePosition, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    MethodItem(s.leftEdge, 0, position) { scope.launch { UserPreferences.setTriggerPosition(context, 0) } }
                    MethodItem(s.rightEdge, 1, position) { scope.launch { UserPreferences.setTriggerPosition(context, 1) } }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.hapticFeedback, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { scope.launch { UserPreferences.setTriggerVibrate(context, it) } },
                            colors = SwitchDefaults.colors()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.fullscreenImmersive, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = widthRatio >= 1.0f,
                            onCheckedChange = { next ->
                                scope.launch { UserPreferences.setSidebarWidthRatio(context, if (next) 1.0f else 0.80f) }
                            },
                            colors = SwitchDefaults.colors()
                        )
                    }
                }
            }

            Button(
                onClick = onOpenArea,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(s.advancedAreaSetup)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MethodItem(label: String, value: Int, current: Int, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp)
    ) {
        RadioButton(selected = value == current, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
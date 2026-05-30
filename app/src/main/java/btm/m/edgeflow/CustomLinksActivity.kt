package btm.m.edgeflow

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import btm.m.edgeflow.data.dao.CustomLinkDao
import btm.m.edgeflow.data.entity.CustomLink
import btm.m.edgeflow.ui.locale.AppLocale
import btm.m.edgeflow.ui.theme.EdgeFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomLinksActivity : ComponentActivity() {

    @Inject lateinit var customLinkDao: CustomLinkDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EdgeFlowTheme {
                CustomLinksScreen(
                    dao = customLinkDao,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomLinksScreen(dao: CustomLinkDao, onBack: () -> Unit) {
    val links by dao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showAddDialog = remember { mutableStateOf(false) }
    val s = AppLocale.strings()

    if (showAddDialog.value) {
        AddLinkDialog(
            onDismiss = { showAddDialog.value = false },
            onAdd = { title, url ->
                scope.launch {
                    dao.insert(CustomLink(title = title, url = url))
                    showAddDialog.value = false
                    Toast.makeText(context, "${s.added}: $title", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.customQuickLinksTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Link")
            }
        }
    ) { padding ->
        if (links.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))
                Text(s.noCustomLinksYet, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.tapPlusToAdd, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(links, key = { it.id }) { link ->
                    LinkItem(link = link, onDelete = {
                        scope.launch { dao.deleteById(link.id) }
                    })
                }
            }
        }
    }
}

@Composable
private fun LinkItem(link: CustomLink, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Default.Link, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(link.title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(link.url, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddLinkDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    val title = remember { mutableStateOf("") }
    val url = remember { mutableStateOf("") }
    val s = AppLocale.strings()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addQuickLink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title.value,
                    onValueChange = { title.value = it },
                    label = { Text(s.title) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url.value,
                    onValueChange = { url.value = it },
                    label = { Text(s.url) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.value.isNotBlank() && url.value.isNotBlank()) onAdd(title.value, url.value) },
                enabled = title.value.isNotBlank() && url.value.isNotBlank()
            ) { Text(s.add) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}
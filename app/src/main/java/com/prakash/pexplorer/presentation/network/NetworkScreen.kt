package com.prakash.pexplorer.presentation.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.NetworkDevice
import com.prakash.pexplorer.domain.model.NetworkFileEntry
import com.prakash.pexplorer.domain.model.NetworkTransferProgress
import com.prakash.pexplorer.presentation.NetworkUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NetworkScreen(
    state: NetworkUiState,
    onDiscover: () -> Unit,
    onRequestChallenge: (NetworkDevice) -> Unit,
    onConnect: (String) -> Unit,
    onOpenDirectory: (String) -> Unit,
    onGoUp: () -> Unit,
    onDownload: (NetworkFileEntry) -> Unit,
    onUpload: () -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.connected) {
                            state.currentPath?.substringAfterLast('/').orEmpty()
                                .ifBlank { stringResource(R.string.network_root) }
                        } else {
                            stringResource(R.string.p_hub)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.connected && state.currentPath != null) onGoUp() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.connected) {
                        IconButton(onClick = onUpload) {
                            Icon(Icons.Filled.Upload, contentDescription = stringResource(R.string.upload))
                        }
                        TextButton(onClick = onDisconnect) {
                            Text(stringResource(R.string.disconnect))
                        }
                    } else {
                        IconButton(onClick = onDiscover) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.discover_devices))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        if (state.connected) {
            RemoteBrowser(
                state = state,
                paddingValues = paddingValues,
                onOpenDirectory = onOpenDirectory,
                onDownload = onDownload
            )
        } else {
            ConnectionContent(
                state = state,
                paddingValues = paddingValues,
                onDiscover = onDiscover,
                onRequestChallenge = onRequestChallenge,
                onConnect = onConnect
            )
        }
    }
}

@Composable
private fun ConnectionContent(
    state: NetworkUiState,
    paddingValues: PaddingValues,
    onDiscover: () -> Unit,
    onRequestChallenge: (NetworkDevice) -> Unit,
    onConnect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(R.string.network_files), style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.network_protocol_note),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDiscover, enabled = !state.isDiscovering) {
                        if (state.isDiscovering) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.discovering_devices))
                        } else {
                            Text(stringResource(R.string.discover_devices))
                        }
                    }
                }
            }
        }
        if (state.isDiscovering) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        state.errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        if (state.challengeCodes.isNotEmpty()) {
            item {
                ChallengeCard(state = state, onConnect = onConnect)
            }
        }
        if (!state.isDiscovering && state.devices.isEmpty() && state.challengeCodes.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_network_devices),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.devices, key = { "${it.host}:${it.port}" }) { device ->
            DeviceRow(device = device, onClick = { onRequestChallenge(device) })
        }
    }
}

@Composable
private fun ChallengeCard(state: NetworkUiState, onConnect: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(stringResource(R.string.authentication_code), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                stringResource(R.string.choose_authentication_code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.challengeCodes.forEach { code ->
                    OutlinedButton(onClick = { onConnect(code) }) {
                        Text(code)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: NetworkDevice, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${device.host}:${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(stringResource(R.string.connect), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RemoteBrowser(
    state: NetworkUiState,
    paddingValues: PaddingValues,
    onOpenDirectory: (String) -> Unit,
    onDownload: (NetworkFileEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        state.download?.let { progress -> DownloadProgress(progress) }
        val entries = if (state.currentPath == null) state.roots else state.files
        if (state.isConnecting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.errorMessage != null) {
            Text(
                state.errorMessage,
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.error
            )
        } else if (entries.isEmpty() && !state.isConnecting) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.network_empty_folder), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(entries, key = { it.path }) { entry ->
                    RemoteEntryRow(
                        entry = entry,
                        isRoot = state.currentPath == null,
                        onOpenDirectory = onOpenDirectory,
                        onDownload = onDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: NetworkTransferProgress) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(progress.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
        Text(
            "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RemoteEntryRow(
    entry: NetworkFileEntry,
    isRoot: Boolean,
    onOpenDirectory: (String) -> Unit,
    onDownload: (NetworkFileEntry) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isRoot || entry.isDirectory) {
                if (isRoot || entry.isDirectory) onOpenDirectory(entry.path)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isRoot || entry.isDirectory) Icons.Filled.Wifi else Icons.Filled.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            if (!isRoot && !entry.isDirectory) {
                Text(
                    formatBytes(entry.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isRoot && !entry.isDirectory) {
            IconButton(onClick = { onDownload(entry) }) {
                Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.download))
            }
        }
    }
}

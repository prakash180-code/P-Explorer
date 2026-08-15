package com.prakash.pexplorer.presentation.analyzer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.DuplicateGroup
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.presentation.DuplicateUiState
import com.prakash.pexplorer.presentation.components.FileVisual

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DuplicateFinderScreen(
    state: DuplicateUiState,
    showFileExtensions: Boolean,
    onScan: () -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onDelete: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        if (state.groups.isEmpty() && !state.scan.isScanning) onScan()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedPaths.isEmpty()) stringResource(R.string.duplicate_files)
                        else pluralStringResource(
                            R.plurals.selected_count,
                            selectedPaths.size,
                            selectedPaths.size
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPaths.isNotEmpty()) selectedPaths = emptySet() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (selectedPaths.isNotEmpty()) {
                        IconButton(onClick = {
                            onDelete(selectedPaths.toList())
                            selectedPaths = emptySet()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    } else {
                        IconButton(onClick = onScan) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.scan_storage))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!state.scan.isScanning && state.groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = {
                        selectedPaths = state.groups
                            .flatMap { it.files.drop(1) }
                            .mapTo(mutableSetOf()) { it.path }
                    }) {
                        Text(stringResource(R.string.select_duplicates))
                    }
                }
            }
            when {
                state.scan.isScanning -> ScanProgressContent(state.scan, PaddingValues(24.dp))
                state.scan.errorMessage != null -> ErrorContent(state.scan.errorMessage, onScan, PaddingValues(24.dp))
                state.groups.isEmpty() -> EmptyDuplicates()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    state.groups.forEachIndexed { index, group ->
                        item(key = "group-$index-${group.key}") {
                            DuplicateGroupHeader(group, index + 1)
                        }
                        items(group.files, key = { it.path }) { file ->
                            DuplicateFileRow(
                                file = file,
                                showFileExtensions = showFileExtensions,
                                isSelected = file.path in selectedPaths,
                                onClick = {
                                    if (selectedPaths.isNotEmpty()) {
                                        selectedPaths = toggle(selectedPaths, file.path)
                                    } else {
                                        onOpenFile(file)
                                    }
                                },
                                onLongClick = { selectedPaths = toggle(selectedPaths, file.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupHeader(group: DuplicateGroup, number: Int) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)) {
        Text(
            text = "${stringResource(R.string.duplicate_group)} $number",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${group.files.firstOrNull()?.name.orEmpty()} • ${stringResource(R.string.same_name_and_size)} • ${formatBytes(group.totalBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DuplicateFileRow(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileVisual(file = file, modifier = Modifier.size(46.dp), iconSize = 24.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (showFileExtensions) file.name else file.name.substringBeforeLast('.', file.name),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                file.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyDuplicates() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.no_duplicates), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun toggle(paths: Set<String>, path: String): Set<String> =
    if (path in paths) paths - path else paths + path

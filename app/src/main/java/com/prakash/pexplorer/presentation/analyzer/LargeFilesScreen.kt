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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.displayFileName
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.presentation.LargeFilesUiState
import com.prakash.pexplorer.presentation.components.FileVisual

private val LARGE_FILE_THRESHOLDS = listOf(
    100L * 1024L * 1024L to R.string.threshold_100_mb,
    500L * 1024L * 1024L to R.string.threshold_500_mb,
    1024L * 1024L * 1024L to R.string.threshold_1_gb
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LargeFilesScreen(
    state: LargeFilesUiState,
    showFileExtensions: Boolean,
    onScan: (Long) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onDelete: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        if (state.files.isEmpty() && !state.scan.isScanning) onScan(state.minimumBytes)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedPaths.isEmpty()) stringResource(R.string.large_files)
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
                        IconButton(onClick = { onScan(state.minimumBytes) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.scan_storage))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LARGE_FILE_THRESHOLDS.forEach { (threshold, label) ->
                    AssistChip(
                        onClick = {
                            selectedPaths = emptySet()
                            onScan(threshold)
                        },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
            when {
                state.scan.isScanning -> ScanProgressContent(state.scan, PaddingValues(24.dp))
                state.scan.errorMessage != null -> ErrorContent(
                    state.scan.errorMessage,
                    { onScan(state.minimumBytes) },
                    PaddingValues(24.dp)
                )
                state.files.isEmpty() -> EmptyLargeFiles()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.files, key = { it.path }) { file ->
                        LargeFileRow(
                            file = file,
                            showFileExtensions = showFileExtensions,
                            isSelected = file.path in selectedPaths,
                            selectionMode = selectedPaths.isNotEmpty(),
                            onClick = {
                                if (selectedPaths.isNotEmpty()) {
                                    selectedPaths = toggle(selectedPaths, file.path)
                                } else {
                                    onOpenFile(file)
                                }
                            },
                            onLongClick = {
                                selectedPaths = toggle(selectedPaths, file.path)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeFileRow(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .alpha(if (isSelected) 1f else 0.96f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileVisual(file = file, modifier = Modifier.size(52.dp), iconSize = 27.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayFileName(file, showFileExtensions),
                style = MaterialTheme.typography.bodyLarge,
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
        Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmptyLargeFiles() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.no_large_files), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun toggle(paths: Set<String>, path: String): Set<String> =
    if (path in paths) paths - path else paths + path

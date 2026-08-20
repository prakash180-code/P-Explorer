package com.prakash.pexplorer.presentation.analyzer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.usecase.FileSorter
import com.prakash.pexplorer.domain.usecase.FolderGrouper
import com.prakash.pexplorer.presentation.LargeFilesUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LargeFilesScreen(
    state: LargeFilesUiState,
    showFileExtensions: Boolean,
    onScan: (Long) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onDelete: (List<String>) -> Unit,
    onShare: (List<ExplorerFile>) -> Unit,
    onBack: () -> Unit
) {
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var groupMode by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.SIZE_LARGEST) }
    BackHandler(onBack = {
        when {
            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
            selectedFolder != null -> selectedFolder = null
            groupMode -> groupMode = false
            else -> onBack()
        }
    })
    LaunchedEffect(Unit) {
        if (state.files.isEmpty() && !state.scan.isScanning) onScan(state.minimumBytes)
    }
    val currentFolder = selectedFolder
    val shownFiles = if (currentFolder != null) {
        FileSorter.sort(
            state.files.filter { FolderGrouper.parentOf(it.path) == currentFolder },
            sortOrder,
            foldersFirst = false
        )
    } else if (groupMode) {
        emptyList()
    } else {
        FileSorter.sort(state.files, sortOrder, foldersFirst = false)
    }
    val selectedFiles = state.files.filter { it.path in selectedPaths }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedPaths.isNotEmpty() -> pluralStringResource(
                                R.plurals.selected_count,
                                selectedPaths.size,
                                selectedPaths.size
                            )
                            currentFolder != null -> currentFolder.substringAfterLast('/').ifEmpty { currentFolder }
                            groupMode -> stringResource(R.string.folder_view)
                            else -> stringResource(R.string.large_files)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
                            currentFolder != null -> selectedFolder = null
                            groupMode -> groupMode = false
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    when {
                        selectedPaths.isNotEmpty() -> {
                            IconButton(onClick = { onShare(selectedFiles) }) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                            }
                            IconButton(onClick = {
                                onDelete(selectedPaths.toList())
                                selectedPaths = emptySet()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                        currentFolder == null && !groupMode -> IconButton(onClick = { onScan(state.minimumBytes) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.scan_storage))
                        }
                        else -> AnalyzerSortMenu(
                            sortOrder = sortOrder,
                            onSortOrderChange = { sortOrder = it },
                            forFolders = groupMode
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (currentFolder == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {
                            selectedPaths = emptySet()
                            groupMode = false
                            onScan(100L * 1024L * 1024L)
                        },
                        label = { Text(stringResource(R.string.threshold_100_mb)) }
                    )
                    AssistChip(
                        onClick = {
                            selectedPaths = emptySet()
                            groupMode = false
                            onScan(500L * 1024L * 1024L)
                        },
                        label = { Text(stringResource(R.string.threshold_500_mb)) }
                    )
                    AssistChip(
                        onClick = {
                            selectedPaths = emptySet()
                            groupMode = false
                            onScan(1024L * 1024L * 1024L)
                        },
                        label = { Text(stringResource(R.string.threshold_1_gb)) }
                    )
                    AssistChip(
                        onClick = {
                            selectedPaths = emptySet()
                            groupMode = !groupMode
                        },
                        label = { Text(stringResource(if (groupMode) R.string.file_view else R.string.folder_view)) }
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
                currentFolder != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(shownFiles, key = { it.path }) { file ->
                        AnalyzerFileRow(
                            file = file,
                            showFileExtensions = showFileExtensions,
                            isSelected = file.path in selectedPaths,
                            selectionMode = selectedPaths.isNotEmpty(),
                            onClick = {
                                if (selectedPaths.isNotEmpty()) {
                                    selectedPaths = togglePath(selectedPaths, file.path)
                                } else {
                                    onOpenFile(file)
                                }
                            },
                            onLongClick = { selectedPaths = togglePath(selectedPaths, file.path) }
                        )
                    }
                }
                groupMode -> {
                    val folders = FolderGrouper.sort(FolderGrouper.group(state.files), sortOrder)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(folders, key = { it.path }) { folder ->
                            FolderUsageRow(
                                usage = folder,
                                totalBytes = state.files.sumOf { it.sizeBytes },
                                onClick = { selectedFolder = folder.path }
                            )
                        }
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(shownFiles, key = { it.path }) { file ->
                        AnalyzerFileRow(
                            file = file,
                            showFileExtensions = showFileExtensions,
                            isSelected = file.path in selectedPaths,
                            selectionMode = selectedPaths.isNotEmpty(),
                            onClick = {
                                if (selectedPaths.isNotEmpty()) {
                                    selectedPaths = togglePath(selectedPaths, file.path)
                                } else {
                                    onOpenFile(file)
                                }
                            },
                            onLongClick = { selectedPaths = togglePath(selectedPaths, file.path) }
                        )
                    }
                }
            }
        }
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
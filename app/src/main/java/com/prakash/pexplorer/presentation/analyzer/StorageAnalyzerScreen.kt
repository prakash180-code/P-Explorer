package com.prakash.pexplorer.presentation.analyzer

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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FolderUsage
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.StorageAnalysis
import com.prakash.pexplorer.domain.model.StorageCategory
import com.prakash.pexplorer.domain.usecase.FileSorter
import com.prakash.pexplorer.domain.usecase.FolderGrouper
import com.prakash.pexplorer.presentation.AnalyzerUiState
import com.prakash.pexplorer.presentation.ScanUiState

private sealed interface AnalyzerLevel {
    data object Overview : AnalyzerLevel
    data class Category(val category: StorageCategory) : AnalyzerLevel
    data class Folder(val category: StorageCategory, val folder: FolderUsage) : AnalyzerLevel
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StorageAnalyzerScreen(
    state: AnalyzerUiState,
    showFileExtensions: Boolean,
    onScan: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onDelete: (List<String>) -> Unit,
    onShare: (List<ExplorerFile>) -> Unit,
    onBack: () -> Unit
) {
    var level by remember { mutableStateOf<AnalyzerLevel>(AnalyzerLevel.Overview) }
    var sortOrder by remember { mutableStateOf(SortOrder.SIZE_LARGEST) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val analysis = state.analysis

    LaunchedEffect(Unit) {
        if (analysis == null && !state.scan.isScanning) onScan()
    }
    LaunchedEffect(analysis) {
        if (analysis == null) level = AnalyzerLevel.Overview
    }
    BackHandler {
        when {
            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
            level is AnalyzerLevel.Folder -> level = (level as AnalyzerLevel.Folder).let {
                AnalyzerLevel.Category(it.category)
            }
            level is AnalyzerLevel.Category -> level = AnalyzerLevel.Overview
            else -> onBack()
        }
    }

    val category = (level as? AnalyzerLevel.Category)?.category
    val folder = (level as? AnalyzerLevel.Folder)?.folder
    val selectedFiles = if (analysis != null && category != null && folder == null) {
        analysis.categoryFiles[category]?.filter { it.path in selectedPaths }.orEmpty()
    } else if (analysis != null && category != null && folder != null) {
        analysis.categoryFiles[category]
            ?.filter { FolderGrouper.parentOf(it.path) == folder.path && it.path in selectedPaths }
            .orEmpty()
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        selectedPaths.isNotEmpty() -> Text(
                            pluralStringResource(R.plurals.selected_count, selectedPaths.size, selectedPaths.size)
                        )
                        folder != null -> Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        category != null -> Text(categoryLabel(category))
                        else -> Text(stringResource(R.string.storage_analyzer))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
                            level is AnalyzerLevel.Folder -> level = AnalyzerLevel.Category(category!!)
                            level is AnalyzerLevel.Category -> level = AnalyzerLevel.Overview
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
                        level != AnalyzerLevel.Overview -> {
                            AnalyzerSortMenu(
                                sortOrder = sortOrder,
                                onSortOrderChange = { sortOrder = it },
                                forFolders = level is AnalyzerLevel.Category
                            )
                        }
                        else -> IconButton(onClick = onScan) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.scan_storage)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        when {
            state.scan.isScanning -> ScanProgressContent(state.scan, paddingValues)
            state.scan.errorMessage != null -> ErrorContent(state.scan.errorMessage, onScan, paddingValues)
            analysis != null -> when (val currentLevel = level) {
                AnalyzerLevel.Overview -> AnalysisContent(
                    analysis = analysis,
                    paddingValues = paddingValues,
                    onOpenCategory = { level = AnalyzerLevel.Category(it) },
                    onOpenLargeFiles = onOpenLargeFiles,
                    onOpenDuplicates = onOpenDuplicates
                )
                is AnalyzerLevel.Category -> CategoryContent(
                    analysis = analysis,
                    category = currentLevel.category,
                    sortOrder = sortOrder,
                    paddingValues = paddingValues,
                    onOpenFolder = { folderUsage ->
                        level = AnalyzerLevel.Folder(currentLevel.category, folderUsage)
                    }
                )
                is AnalyzerLevel.Folder -> FolderContent(
                    analysis = analysis,
                    category = currentLevel.category,
                    folder = currentLevel.folder,
                    sortOrder = sortOrder,
                    showFileExtensions = showFileExtensions,
                    selectedPaths = selectedPaths,
                    paddingValues = paddingValues,
                    onToggleSelection = { path ->
                        selectedPaths = togglePath(selectedPaths, path)
                    },
                    onOpenFile = onOpenFile
                )
            }
            else -> EmptyAnalyzer(onScan, paddingValues)
        }
    }
}

@Composable
fun ScanProgressContent(scan: ScanUiState, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(stringResource(R.string.scan_in_progress), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(modifier = Modifier.width(180.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            pluralStringResource(R.plurals.scanned_file_count, scan.scannedFiles, scan.scannedFiles),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        scan.currentPath?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun ErrorContent(message: String, onScan: () -> Unit, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onScan) { Text(stringResource(R.string.try_again)) }
    }
}

@Composable
private fun AnalysisContent(
    analysis: StorageAnalysis,
    paddingValues: PaddingValues,
    onOpenCategory: (StorageCategory) -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenDuplicates: () -> Unit
) {
    val usedFraction = if (analysis.totalBytes > 0) {
        (analysis.usedBytes.toDouble() / analysis.totalBytes).toFloat()
    } else {
        0f
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.storage_usage), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.total_used_free,
                            formatBytes(analysis.usedBytes),
                            formatBytes(analysis.totalBytes),
                            formatBytes(analysis.freeBytes)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
        item { Text(stringResource(R.string.storage_usage), style = MaterialTheme.typography.titleMedium) }
        items(analysis.categories, key = { it.category }) { usage ->
            CategoryRow(
                category = usage.category,
                bytes = usage.bytes,
                count = usage.fileCount,
                totalUsedBytes = analysis.usedBytes,
                onClick = { onOpenCategory(usage.category) }
            )
        }
        item {
            Text(stringResource(R.string.largest_files), style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedButton(onClick = onOpenLargeFiles, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.find_large_files))
            }
        }
        item {
            OutlinedButton(onClick = onOpenDuplicates, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.find_duplicates))
            }
        }
        item {
            Text(
                pluralStringResource(
                    R.plurals.scanned_file_count,
                    analysis.scannedFileCount,
                    analysis.scannedFileCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryContent(
    analysis: StorageAnalysis,
    category: StorageCategory,
    sortOrder: SortOrder,
    paddingValues: PaddingValues,
    onOpenFolder: (FolderUsage) -> Unit
) {
    val categoryFiles = analysis.categoryFiles[category].orEmpty()
    val folders = FolderGrouper.sort(FolderGrouper.group(categoryFiles), sortOrder)
    val totalBytes = categoryFiles.sumOf { it.sizeBytes }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            CategoryHeader(
                category = category,
                bytes = totalBytes,
                count = categoryFiles.size,
                totalUsedBytes = analysis.usedBytes
            )
        }
        if (folders.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_files_in_category),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(folders, key = { it.path }) { usage ->
            FolderUsageRow(
                usage = usage,
                totalBytes = totalBytes,
                onClick = { onOpenFolder(usage) }
            )
        }
    }
}

@Composable
private fun FolderContent(
    analysis: StorageAnalysis,
    category: StorageCategory,
    folder: FolderUsage,
    sortOrder: SortOrder,
    showFileExtensions: Boolean,
    selectedPaths: Set<String>,
    paddingValues: PaddingValues,
    onToggleSelection: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit
) {
    val folderFiles = analysis.categoryFiles[category].orEmpty()
        .filter { FolderGrouper.parentOf(it.path) == folder.path }
    val sortedFiles = FileSorter.sort(folderFiles, sortOrder, foldersFirst = false)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "${formatBytes(folder.bytes)} • " +
                        pluralStringResource(R.plurals.file_count, folder.fileCount, folder.fileCount),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (sortedFiles.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_files_in_category),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(sortedFiles, key = { it.path }) { file ->
            AnalyzerFileRow(
                file = file,
                showFileExtensions = showFileExtensions,
                isSelected = file.path in selectedPaths,
                selectionMode = selectedPaths.isNotEmpty(),
                onClick = {
                    if (selectedPaths.isNotEmpty()) {
                        onToggleSelection(file.path)
                    } else {
                        onOpenFile(file)
                    }
                },
                onLongClick = { onToggleSelection(file.path) }
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    category: StorageCategory,
    bytes: Long,
    count: Int,
    totalUsedBytes: Long
) {
    val fraction = if (totalUsedBytes > 0) (bytes.toDouble() / totalUsedBytes).toFloat() else 0f
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(categoryLabel(category), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatBytes(bytes)} • " +
                    pluralStringResource(R.plurals.file_count, count, count),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: StorageCategory,
    bytes: Long,
    count: Int,
    totalUsedBytes: Long,
    onClick: () -> Unit
) {
    val fraction = if (totalUsedBytes > 0) (bytes.toDouble() / totalUsedBytes).toFloat() else 0f
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(categoryLabel(category), modifier = Modifier.weight(1f))
            Text(formatBytes(bytes), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            pluralStringResource(R.plurals.file_count, count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun categoryLabel(category: StorageCategory): String = stringResource(
    when (category) {
        StorageCategory.IMAGES -> R.string.storage_category_images
        StorageCategory.VIDEOS -> R.string.storage_category_videos
        StorageCategory.AUDIO -> R.string.storage_category_audio
        StorageCategory.DOCUMENTS -> R.string.storage_category_documents
        StorageCategory.DOWNLOADS -> R.string.storage_category_downloads
        StorageCategory.APPS -> R.string.storage_category_apps
        StorageCategory.ARCHIVES -> R.string.storage_category_archives
        StorageCategory.OTHER -> R.string.storage_category_other
    }
)

@Composable
private fun EmptyAnalyzer(onScan: () -> Unit, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onScan) { Text(stringResource(R.string.scan_storage)) }
    }
}
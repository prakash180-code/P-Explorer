package com.prakash.pexplorer.presentation.analyzer

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.StorageAnalysis
import com.prakash.pexplorer.domain.model.StorageCategory
import com.prakash.pexplorer.presentation.AnalyzerUiState
import com.prakash.pexplorer.presentation.ScanUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StorageAnalyzerScreen(
    state: AnalyzerUiState,
    onScan: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        if (state.analysis == null && !state.scan.isScanning) onScan()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage_analyzer)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.scan_storage))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        when {
            state.scan.isScanning -> ScanProgressContent(state.scan, paddingValues)
            state.scan.errorMessage != null -> ErrorContent(state.scan.errorMessage, onScan, paddingValues)
            state.analysis != null -> AnalysisContent(
                analysis = state.analysis,
                paddingValues = paddingValues,
                onOpenLargeFiles = onOpenLargeFiles,
                onOpenDuplicates = onOpenDuplicates
            )
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
            CategoryRow(usage.category, usage.bytes, usage.fileCount, analysis.usedBytes)
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
private fun CategoryRow(
    category: StorageCategory,
    bytes: Long,
    count: Int,
    totalUsedBytes: Long
) {
    val fraction = if (totalUsedBytes > 0) (bytes.toDouble() / totalUsedBytes).toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(categoryLabel(category), modifier = Modifier.weight(1f))
            Text(formatBytes(bytes), style = MaterialTheme.typography.labelLarge)
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

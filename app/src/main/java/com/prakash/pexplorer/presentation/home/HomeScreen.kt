package com.prakash.pexplorer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.presentation.ExplorerUiState
import com.prakash.pexplorer.presentation.navigation.UtilityDestination
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: ExplorerUiState,
    onOpenFiles: () -> Unit,
    onOpenSearch: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenStorage: (String) -> Unit,
    onRequestStorageAccess: () -> Unit,
    onOpenUtility: (UtilityDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HomeHeader(onOpenUtility = onOpenUtility)
        }

        item {
            SectionTitle(title = stringResource(R.string.storage))
        }

        item {
            StorageSection(
                state = state,
                onOpenStorage = onOpenStorage,
                onRequestStorageAccess = onRequestStorageAccess
            )
        }

        item {
            SectionTitle(title = stringResource(R.string.quick_actions))
        }

        item {
            QuickActions(
                onOpenSearch = onOpenSearch,
                onCreateFolder = onCreateFolder,
                onOpenAnalyzer = { onOpenUtility(UtilityDestination.ANALYZER) }
            )
        }

        item {
            SectionTitle(title = stringResource(R.string.quick_categories))
        }

        item {
            QuickCategories(onOpenFiles = onOpenFiles)
        }

        item {
            SectionTitle(
                title = stringResource(R.string.recent_files),
                trailing = stringResource(R.string.see_all)
            )
        }

        item {
            RecentEmptyCard(onOpenFiles = onOpenFiles)
        }
    }
}

@Composable
private fun HomeHeader(onOpenUtility: (UtilityDestination) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_options)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            UtilityDestination.entries.forEach { destination ->
                DropdownMenuItem(
                    text = { Text(stringResource(destination.titleRes)) },
                    leadingIcon = {
                        Icon(imageVector = destination.icon, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenUtility(destination)
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StorageSection(
    state: ExplorerUiState,
    onOpenStorage: (String) -> Unit,
    onRequestStorageAccess: () -> Unit
) {
    when {
        !state.storageAccessGranted -> StorageAccessCard(onRequestStorageAccess)
        state.storageLoading && state.storage.isEmpty() -> LoadingStorageCard()
        state.storage.isEmpty() -> {
            Text(
                text = state.storageError ?: stringResource(R.string.no_storage_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.storage.chunked(2).forEach { rowStorages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowStorages.forEach { storage ->
                            StorageCard(
                                storage = storage,
                                onClick = { onOpenStorage(storage.path) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowStorages.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageAccessCard(onRequestStorageAccess: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.storage_access_needed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.storage_access_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestStorageAccess) {
                Text(text = stringResource(R.string.grant_access))
            }
        }
    }
}

@Composable
private fun LoadingStorageCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            LinearProgressIndicator(modifier = Modifier.width(120.dp))
        }
    }
}

@Composable
private fun StorageCard(
    storage: StorageInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (storage.isRemovable) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = storage.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.used_of_total,
                            formatBytes(storage.usedBytes),
                            formatBytes(storage.totalBytes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                val freePercentage = if (storage.totalBytes > 0L) {
                    (storage.freeBytes.toDouble() / storage.totalBytes * 100).roundToInt()
                } else {
                    0
                }
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { storage.usedFraction },
                        modifier = Modifier.fillMaxSize(),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = stringResource(R.string.free_percent, freePercentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.free_space, formatBytes(storage.freeBytes)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickActions(
    onOpenSearch: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenAnalyzer: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssistChip(
            onClick = onOpenSearch,
            label = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        )
        AssistChip(
            onClick = onCreateFolder,
            label = { Text(stringResource(R.string.create_folder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        )
        AssistChip(
            onClick = onOpenAnalyzer,
            label = { Text(stringResource(R.string.storage_analyzer)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        )
    }
}

private data class QuickCategory(
    val labelRes: Int,
    val icon: ImageVector,
    val tint: Color
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickCategories(onOpenFiles: () -> Unit) {
    val categories = remember {
        listOf(
            QuickCategory(R.string.images, Icons.Filled.Image, Color(0xFF4F8EF7)),
            QuickCategory(R.string.videos, Icons.Filled.Movie, Color(0xFF8E62B1)),
            QuickCategory(R.string.audio, Icons.Filled.MusicNote, Color(0xFFFF9800)),
            QuickCategory(R.string.documents, Icons.Filled.Description, Color(0xFF55677A)),
            QuickCategory(R.string.downloads, Icons.Filled.Download, Color(0xFF1E88E5)),
            QuickCategory(R.string.apks, Icons.Filled.Android, Color(0xFF3D8B6D)),
            QuickCategory(R.string.archives, Icons.Filled.Archive, Color(0xFFF57C00))
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { category ->
            Card(
                onClick = onOpenFiles,
                modifier = Modifier.width(88.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.tint,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(category.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentEmptyCard(onOpenFiles: () -> Unit) {
    Card(
        onClick = onOpenFiles,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.recent_files_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.browse_files),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

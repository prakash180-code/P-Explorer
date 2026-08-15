package com.prakash.pexplorer.presentation.library

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.prakash.pexplorer.core.util.displayFileName
import com.prakash.pexplorer.core.util.formatModifiedDate
import com.prakash.pexplorer.domain.model.FileReference
import com.prakash.pexplorer.domain.model.toExplorerFile
import com.prakash.pexplorer.presentation.StoredFileUi
import com.prakash.pexplorer.presentation.components.FileVisual

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FavoritesLibraryScreen(
    items: List<StoredFileUi>,
    showFileExtensions: Boolean,
    onOpen: (StoredFileUi) -> Unit,
    onRemove: (String) -> Unit,
    onOpenFiles: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LibraryContent(
            items = items,
            showFileExtensions = showFileExtensions,
            emptyDescription = stringResource(R.string.favorites_empty_description),
            emptyIcon = Icons.Filled.Favorite,
            onOpen = onOpen,
            onRemove = onRemove,
            onOpenFiles = onOpenFiles,
            paddingValues = paddingValues
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecentLibraryScreen(
    items: List<StoredFileUi>,
    showFileExtensions: Boolean,
    onOpen: (StoredFileUi) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onOpenFiles: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recent)) },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = onClearAll) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LibraryContent(
            items = items,
            showFileExtensions = showFileExtensions,
            emptyDescription = stringResource(R.string.recent_empty_description),
            emptyIcon = Icons.Filled.History,
            onOpen = onOpen,
            onRemove = onRemove,
            onOpenFiles = onOpenFiles,
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun LibraryContent(
    items: List<StoredFileUi>,
    showFileExtensions: Boolean,
    emptyDescription: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onOpen: (StoredFileUi) -> Unit,
    onRemove: (String) -> Unit,
    onOpenFiles: () -> Unit,
    paddingValues: PaddingValues
) {
    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = emptyIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(58.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = emptyDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onOpenFiles) {
                Text(stringResource(R.string.browse_files))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items, key = { it.reference.path }) { item ->
                LibraryItemRow(
                    item = item,
                    showFileExtensions = showFileExtensions,
                    onOpen = { onOpen(item) },
                    onRemove = { onRemove(item.reference.path) }
                )
            }
        }
    }
}

@Composable
private fun LibraryItemRow(
    item: StoredFileUi,
    showFileExtensions: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val reference = item.reference
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isAvailable, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileVisual(
            file = reference.toExplorerFile(),
            modifier = Modifier.size(52.dp),
            iconSize = 27.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayFileName(reference.toExplorerFile(), showFileExtensions),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (item.isAvailable) {
                    reference.path
                } else {
                    stringResource(R.string.unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            reference.lastAccessedEpochMillis?.let { accessed ->
                Text(
                    text = formatModifiedDate(accessed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.remove)
            )
        }
    }
}

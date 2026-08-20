package com.prakash.pexplorer.presentation.analyzer

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.displayFileName
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FolderUsage
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.presentation.components.FileVisual

@Composable
fun AnalyzerSortMenu(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    forFolders: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.sort_by))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = if (forFolders) {
                listOf(SortOrder.SIZE_LARGEST, SortOrder.SIZE_SMALLEST, SortOrder.NAME_ASC, SortOrder.NAME_DESC)
            } else {
                SortOrder.entries
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(sortOptionLabel(option))) },
                    leadingIcon = {
                        Icon(sortOptionIcon(option), contentDescription = null)
                    },
                    trailingIcon = {
                        if (option == sortOrder) {
                            Icon(
                                sortDirectionIcon(option) ?: Icons.Filled.SwapVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortOrderChange(option)
                    }
                )
            }
        }
    }
}

@Composable
fun FolderUsageRow(
    usage: FolderUsage,
    totalBytes: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (totalBytes > 0) (usage.bytes.toDouble() / totalBytes).toFloat() else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usage.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = usage.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pluralStringResource(R.plurals.file_count, usage.fileCount, usage.fileCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatBytes(usage.bytes),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AnalyzerFileRow(
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

fun togglePath(paths: Set<String>, path: String): Set<String> =
    if (path in paths) paths - path else paths + path

private fun sortOptionIcon(sortOrder: SortOrder): ImageVector = when (sortOrder) {
    SortOrder.NAME_ASC, SortOrder.NAME_DESC -> Icons.Filled.SortByAlpha
    SortOrder.TYPE -> Icons.Filled.Description
    SortOrder.SIZE_SMALLEST, SortOrder.SIZE_LARGEST -> Icons.Filled.SwapVert
    SortOrder.DATE_NEWEST, SortOrder.DATE_OLDEST -> Icons.Filled.Update
    SortOrder.CREATED_NEWEST -> Icons.Filled.CalendarToday
    SortOrder.CREATED_OLDEST -> Icons.Filled.Event
}

private fun sortDirectionIcon(sortOrder: SortOrder): ImageVector? = when (sortOrder) {
    SortOrder.NAME_ASC, SortOrder.SIZE_SMALLEST -> Icons.Filled.ArrowUpward
    SortOrder.NAME_DESC, SortOrder.SIZE_LARGEST -> Icons.Filled.ArrowDownward
    SortOrder.DATE_NEWEST, SortOrder.CREATED_NEWEST -> Icons.Filled.ArrowUpward
    SortOrder.DATE_OLDEST, SortOrder.CREATED_OLDEST -> Icons.Filled.ArrowDownward
    SortOrder.TYPE -> null
}

private fun sortOptionLabel(sortOrder: SortOrder): Int = when (sortOrder) {
    SortOrder.NAME_ASC -> R.string.name_a_z
    SortOrder.NAME_DESC -> R.string.name_z_a
    SortOrder.TYPE -> R.string.file_type
    SortOrder.SIZE_SMALLEST -> R.string.size_smallest
    SortOrder.SIZE_LARGEST -> R.string.size_largest
    SortOrder.DATE_NEWEST -> R.string.date_newest
    SortOrder.DATE_OLDEST -> R.string.date_oldest
    SortOrder.CREATED_NEWEST -> R.string.created_newest
    SortOrder.CREATED_OLDEST -> R.string.created_oldest
}
package com.prakash.pexplorer.presentation.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.ViewStyle

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ViewSettingsDialog(
    initialStyle: ViewStyle,
    initialSortOrder: SortOrder,
    initialFoldersFirst: Boolean,
    onDismiss: () -> Unit,
    onApply: (ViewStyle, SortOrder, Boolean) -> Unit
) {
    var style by remember { mutableStateOf(initialStyle) }
    var sortOrder by remember { mutableStateOf(initialSortOrder) }
    var foldersFirst by remember { mutableStateOf(initialFoldersFirst) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.view_options)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DialogSectionTitle(stringResource(R.string.view_style_section))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewStyle.entries.forEach { entry ->
                        ViewStyleOption(
                            style = entry,
                            selected = entry == style,
                            onClick = { style = entry },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(18.dp))
                DialogSectionTitle(stringResource(R.string.sort_by_section))
                SortOrder.entries.forEach { entry ->
                    SortOptionRow(
                        sortOrder = entry,
                        selected = entry == sortOrder,
                        onClick = { sortOrder = entry }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { foldersFirst = !foldersFirst }
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.folders_first),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = foldersFirst, onCheckedChange = { foldersFirst = it })
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(style, sortOrder, foldersFirst) }
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}

@Composable
private fun DialogSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ViewStyleOption(
    style: ViewStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(
                    imageVector = viewStyleIcon(style),
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = stringResource(viewStyleLabel(style)),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SortOptionRow(
    sortOrder: SortOrder,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = sortOptionIcon(sortOrder),
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(sortOptionLabel(sortOrder)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (selected) {
            Icon(
                imageVector = sortDirectionIcon(sortOrder) ?: Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun viewStyleIcon(style: ViewStyle): ImageVector = when (style) {
    ViewStyle.SMALL_ICON -> Icons.Filled.GridView
    ViewStyle.MEDIUM_ICON -> Icons.Filled.Dashboard
    ViewStyle.LARGE_ICON -> Icons.Filled.Apps
    ViewStyle.LIST -> Icons.AutoMirrored.Filled.ViewList
    ViewStyle.COMPACT_LIST -> Icons.Filled.Reorder
    ViewStyle.DETAILED_LIST -> Icons.Filled.ViewHeadline
    ViewStyle.GRID -> Icons.Filled.GridView
    ViewStyle.TILE -> Icons.Filled.ViewModule
    ViewStyle.CARD_VIEW -> Icons.Filled.ViewAgenda
    ViewStyle.MASONRY -> Icons.AutoMirrored.Filled.ViewQuilt
    ViewStyle.COLUMNS -> Icons.Filled.ViewColumn
    ViewStyle.GALLERY -> Icons.Filled.Image
}

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

private fun viewStyleLabel(style: ViewStyle): Int = when (style) {
    ViewStyle.SMALL_ICON -> R.string.view_small_icon
    ViewStyle.MEDIUM_ICON -> R.string.view_medium_icon
    ViewStyle.LARGE_ICON -> R.string.view_large_icon
    ViewStyle.LIST -> R.string.view_list
    ViewStyle.COMPACT_LIST -> R.string.view_compact_list
    ViewStyle.DETAILED_LIST -> R.string.view_detailed_list
    ViewStyle.GRID -> R.string.view_grid
    ViewStyle.TILE -> R.string.view_tile
    ViewStyle.CARD_VIEW -> R.string.view_card
    ViewStyle.MASONRY -> R.string.view_masonry
    ViewStyle.COLUMNS -> R.string.view_columns
    ViewStyle.GALLERY -> R.string.view_gallery
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

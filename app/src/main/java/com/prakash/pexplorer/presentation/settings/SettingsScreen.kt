package com.prakash.pexplorer.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.ExplorerPreferences
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.ThemeMode
import com.prakash.pexplorer.domain.model.ViewStyle
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.presentation.browser.ViewSettingsDialog

private enum class SettingsChoice {
    THEME,
    VIEW,
    SORT
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    preferences: ExplorerPreferences,
    storage: List<StorageInfo>,
    onBack: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onViewModeChanged: (ViewStyle) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onFoldersFirstChanged: (Boolean) -> Unit,
    onShowHiddenChanged: (Boolean) -> Unit,
    onShowExtensionsChanged: (Boolean) -> Unit,
    onConfirmDeleteChanged: (Boolean) -> Unit,
    onConfirmOverwriteChanged: (Boolean) -> Unit,
    onRememberLastFolderChanged: (Boolean) -> Unit,
    onRecentItemsChanged: (Boolean) -> Unit,
    onClearCache: () -> Unit
) {
    var choice by remember { mutableStateOf<SettingsChoice?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item { SettingsSectionTitle(stringResource(R.string.appearance)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.theme),
                    summary = themeLabel(preferences.themeMode),
                    onClick = { choice = SettingsChoice.THEME }
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.browser_preferences)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.default_view),
                    summary = viewStyleLabel(preferences.viewStyle),
                    onClick = { choice = SettingsChoice.VIEW }
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.sort_by),
                    summary = sortLabel(preferences.sortOrder),
                    onClick = { choice = SettingsChoice.SORT }
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.folders_first),
                    checked = preferences.foldersFirst,
                    onCheckedChange = onFoldersFirstChanged
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.show_hidden_files),
                    checked = preferences.showHiddenFiles,
                    onCheckedChange = onShowHiddenChanged
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.show_file_extensions),
                    checked = preferences.showFileExtensions,
                    onCheckedChange = onShowExtensionsChanged
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.behavior)) }
            item {
                SwitchRow(
                    title = stringResource(R.string.confirm_before_delete),
                    checked = preferences.confirmBeforeDelete,
                    onCheckedChange = onConfirmDeleteChanged
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.confirm_before_overwrite),
                    checked = preferences.confirmBeforeOverwrite,
                    onCheckedChange = onConfirmOverwriteChanged
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.remember_last_folder),
                    checked = preferences.rememberLastFolder,
                    onCheckedChange = onRememberLastFolderChanged
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.recent_items),
                    checked = preferences.recentItemsEnabled,
                    onCheckedChange = onRecentItemsChanged
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.storage_preferences)) }
            item {
                StorageSummary(storage = storage)
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.clear_cache),
                    summary = stringResource(R.string.search_files),
                    onClick = onClearCache
                )
            }
        }
    }

    when (choice) {
        SettingsChoice.THEME -> ChoiceDialog(
            title = stringResource(R.string.theme),
            selected = preferences.themeMode,
            options = ThemeMode.entries,
            label = ::themeLabel,
            onSelected = {
                onThemeModeChanged(it)
                choice = null
            },
            onDismiss = { choice = null }
        )
        SettingsChoice.VIEW -> ViewSettingsDialog(
            initialStyle = preferences.viewStyle,
            initialSortOrder = preferences.sortOrder,
            initialFoldersFirst = preferences.foldersFirst,
            onDismiss = { choice = null },
            onApply = { style, sort, folders ->
                onViewModeChanged(style)
                onSortOrderChanged(sort)
                onFoldersFirstChanged(folders)
                choice = null
            }
        )
        SettingsChoice.SORT -> ChoiceDialog(
            title = stringResource(R.string.sort_by),
            selected = preferences.sortOrder,
            options = SortOrder.entries,
            label = ::sortLabel,
            onSelected = {
                onSortOrderChanged(it)
                choice = null
            },
            onDismiss = { choice = null }
        )
        null -> Unit
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 8.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsRow(title: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Text(
            summary,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StorageSummary(storage: List<StorageInfo>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        storage.forEach { info ->
            Text(info.label, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.used_of_total,
                    formatBytes(info.usedBytes),
                    formatBytes(info.totalBytes)
                ),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (storage.isEmpty()) {
            Text(stringResource(R.string.no_storage_found))
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = { onSelected(option) }
                        )
                        Text(label(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.system_default
        ThemeMode.LIGHT -> R.string.light
        ThemeMode.DARK -> R.string.dark
    }
)

@Composable
private fun viewStyleLabel(viewStyle: ViewStyle): String = stringResource(
    when (viewStyle) {
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
)

@Composable
private fun sortLabel(sortOrder: SortOrder): String = stringResource(
    when (sortOrder) {
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
)

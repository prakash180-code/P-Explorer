package com.prakash.pexplorer.presentation.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.core.util.formatModifiedDate
import com.prakash.pexplorer.core.util.displayFileName
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.ViewStyle
import com.prakash.pexplorer.presentation.BrowserUiState
import com.prakash.pexplorer.presentation.PropertiesUiState
import com.prakash.pexplorer.presentation.TransferUiState
import com.prakash.pexplorer.presentation.components.DeleteConfirmationDialog
import com.prakash.pexplorer.presentation.components.ExtractDialog
import com.prakash.pexplorer.presentation.components.FileVisual
import com.prakash.pexplorer.presentation.components.NameDialog
import com.prakash.pexplorer.presentation.components.PropertiesDialog
import com.prakash.pexplorer.presentation.components.TransferDestinationDialog
import com.prakash.pexplorer.presentation.components.TransferProgressDialog
import com.prakash.pexplorer.presentation.components.ZipNameDialog
import java.io.File

@Composable
fun FileBrowserScreen(
    browserState: BrowserUiState,
    rootPath: String,
    rootLabel: String,
    viewStyle: ViewStyle,
    sortOrder: SortOrder,
    foldersFirst: Boolean,
    showFileExtensions: Boolean,
    confirmBeforeDelete: Boolean,
    storageAccessGranted: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateUp: () -> Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    onRequestStorageAccess: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onApplyViewSettings: (ViewStyle, SortOrder, Boolean) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (List<String>) -> Unit,
    onCopy: (List<String>, String) -> Unit,
    onMove: (List<String>, String) -> Unit,
    onCompress: (List<String>, String) -> Unit,
    onExtract: (String, Boolean) -> Unit,
    onShare: (List<ExplorerFile>) -> Unit,
    onToggleFavorite: (ExplorerFile) -> Unit,
    isFavorite: (String) -> Boolean,
    onShowProperties: (String) -> Unit,
    transfer: TransferUiState?,
    properties: PropertiesUiState?,
    onCancelTransfer: () -> Unit,
    onDismissTransfer: () -> Unit,
    onDismissProperties: () -> Unit,
    initiallyShowCreateFolder: Boolean,
    onCreateFolderRequestConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAtRoot = browserState.path == rootPath
    val selectedFiles = remember(browserState.items, browserState.selectedPaths) {
        browserState.items.filter { it.path in browserState.selectedPaths }
    }
    val selectionMode = selectedFiles.isNotEmpty()
    var showCreateFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ExplorerFile?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var transferOperation by remember { mutableStateOf<FileOperation?>(null) }
    var showViewDialog by remember { mutableStateOf(false) }
    var showZipNameDialog by remember { mutableStateOf(false) }
    var extractTarget by remember { mutableStateOf<ExplorerFile?>(null) }
    LaunchedEffect(initiallyShowCreateFolder) {
        if (initiallyShowCreateFolder) {
            showCreateFolder = true
            onCreateFolderRequestConsumed()
        }
    }
    val navigateBackOrUp = {
        if (isAtRoot || !onNavigateUp()) {
            onNavigateBack()
        }
    }

    BackHandler {
        if (selectionMode) onClearSelection() else navigateBackOrUp()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrowserTopBar(
                browserState = browserState,
                rootLabel = rootLabel,
                selectionMode = selectionMode,
                selectedCount = selectedFiles.size,
                selectedFavorite = selectedFiles.singleOrNull()?.path?.let(isFavorite) == true,
                viewStyle = viewStyle,
                onBack = navigateBackOrUp,
                onOpenViewSettings = { showViewDialog = true },
                onOpenSearch = onOpenSearch,
                onRefresh = onRefresh,
                onClearSelection = onClearSelection,
                onShare = { onShare(selectedFiles) },
                onCopy = { transferOperation = FileOperation.COPY },
                onMove = { transferOperation = FileOperation.MOVE },
                onDelete = {
                    if (confirmBeforeDelete) {
                        showDeleteConfirmation = true
                    } else {
                        onDelete(selectedFiles.map(ExplorerFile::path))
                    }
                },
                onToggleFavorite = {
                    selectedFiles.singleOrNull()?.let(onToggleFavorite)
                },
                selectedIsArchive = selectedFiles.singleOrNull()?.name?.endsWith(".zip", ignoreCase = true) == true,
                onCompress = { showZipNameDialog = true },
                onExtract = { extractTarget = selectedFiles.singleOrNull() },
                onSelectAll = onSelectAll,
                onCreateFolder = { showCreateFolder = true },
                onRename = { renameTarget = selectedFiles.singleOrNull() },
                onOpenSelected = {
                    selectedFiles.singleOrNull()?.let { selected ->
                        if (selected.isDirectory) onOpenDirectory(selected.path) else onOpenFile(selected)
                    }
                },
                onShowProperties = {
                    selectedFiles.singleOrNull()?.path?.let(onShowProperties)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!selectionMode && transfer == null) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { showCreateFolder = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = stringResource(R.string.create_folder)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Breadcrumbs(
                path = browserState.path,
                rootPath = rootPath,
                rootLabel = rootLabel,
                onPathClick = onOpenDirectory
            )
            BrowserContent(
                browserState = browserState,
                viewStyle = viewStyle,
                showFileExtensions = showFileExtensions,
                storageAccessGranted = storageAccessGranted,
                onOpenFile = onOpenFile,
                onToggleSelection = onToggleSelection,
                onOpenDirectory = onOpenDirectory,
                onRefresh = onRefresh,
                onRequestStorageAccess = onRequestStorageAccess
            )
        }
    }

    if (showCreateFolder) {
        NameDialog(
            title = stringResource(R.string.create_folder),
            initialName = "",
            confirmLabel = stringResource(R.string.create_folder),
            onDismiss = { showCreateFolder = false },
            onConfirm = { name ->
                showCreateFolder = false
                onCreateFolder(name)
            }
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = stringResource(R.string.rename),
            initialName = target.name,
            confirmLabel = stringResource(R.string.rename),
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                onRename(target.path, name)
            }
        )
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            selectedCount = selectedFiles.size,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(selectedFiles.map(ExplorerFile::path))
            }
        )
    }

    transferOperation?.let { operation ->
        TransferDestinationDialog(
            operation = operation,
            selectedCount = selectedFiles.size,
            currentPath = browserState.path,
            onDismiss = { transferOperation = null },
            onConfirm = { destination ->
                transferOperation = null
                val paths = selectedFiles.map(ExplorerFile::path)
                if (operation == FileOperation.COPY) onCopy(paths, destination) else onMove(paths, destination)
            }
        )
    }

    transfer?.let { state ->
        TransferProgressDialog(
            state = state,
            onCancel = onCancelTransfer,
            onDismiss = onDismissTransfer
        )
    }

    properties?.let { state ->
        PropertiesDialog(state = state, onDismiss = onDismissProperties)
    }

    if (showViewDialog) {
        ViewSettingsDialog(
            initialStyle = viewStyle,
            initialSortOrder = sortOrder,
            initialFoldersFirst = foldersFirst,
            onDismiss = { showViewDialog = false },
            onApply = { style, sort, folders ->
                showViewDialog = false
                onApplyViewSettings(style, sort, folders)
            }
        )
    }

    if (showZipNameDialog) {
        ZipNameDialog(
            onDismiss = { showZipNameDialog = false },
            onConfirm = { archiveName ->
                showZipNameDialog = false
                onCompress(selectedFiles.map(ExplorerFile::path), archiveName)
            }
        )
    }

    extractTarget?.let { archive ->
        ExtractDialog(
            archiveName = archive.name,
            onDismiss = { extractTarget = null },
            onExtractHere = {
                extractTarget = null
                onExtract(archive.path, false)
            },
            onExtractToNewFolder = {
                extractTarget = null
                onExtract(archive.path, true)
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BrowserTopBar(
    browserState: BrowserUiState,
    rootLabel: String,
    selectionMode: Boolean,
    selectedCount: Int,
    selectedFavorite: Boolean,
    selectedIsArchive: Boolean,
    viewStyle: ViewStyle,
    onBack: () -> Unit,
    onOpenViewSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    onClearSelection: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onSelectAll: () -> Unit,
    onCreateFolder: () -> Unit,
    onRename: () -> Unit,
    onOpenSelected: () -> Unit,
    onShowProperties: () -> Unit
) {
    var moreExpanded by remember { mutableStateOf(false) }
    val folderName = remember(browserState.path) {
        File(browserState.path).name.ifBlank { "" }
    }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = if (selectionMode) onClearSelection else onBack) {
                Icon(
                    imageVector = if (selectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(if (selectionMode) R.string.cancel else R.string.back)
                )
            }
        },
        title = {
            Column {
                Text(
                    text = if (selectionMode) {
                        pluralStringResource(R.plurals.selected_count, selectedCount, selectedCount)
                    } else if (folderName.isBlank()) {
                        rootLabel
                    } else {
                        folderName
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selectionMode) folderName.ifBlank { rootLabel } else {
                        pluralStringResource(R.plurals.items_count, browserState.items.size, browserState.items.size)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            if (selectionMode) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                }
            } else {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                }
                IconButton(onClick = onOpenViewSettings) {
                    Icon(
                        imageVector = if (isListFamily(viewStyle)) {
                            Icons.Filled.GridView
                        } else {
                            Icons.AutoMirrored.Filled.ViewList
                        },
                        contentDescription = stringResource(R.string.view_options),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
            }
            Box {
                IconButton(onClick = { moreExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false }
                ) {
                    if (selectionMode) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy)) },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onMove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.select_all)) },
                            leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onSelectAll()
                            }
                        )
                        if (selectedCount == 1) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.open_with)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                                onClick = {
                                    moreExpanded = false
                                    onOpenSelected()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    moreExpanded = false
                                    onRename()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.properties)) },
                                onClick = {
                                    moreExpanded = false
                                    onShowProperties()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (selectedFavorite) {
                                                R.string.remove_from_favorites
                                            } else {
                                                R.string.add_to_favorites
                                            }
                                        )
                                    )
                                },
                                onClick = {
                                    moreExpanded = false
                                    onToggleFavorite()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.compress)) },
                            leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onCompress()
                            }
                        )
                        if (selectedIsArchive && selectedCount == 1) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.extract)) },
                                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                                onClick = {
                                    moreExpanded = false
                                    onExtract()
                                }
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.create_folder)) },
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onCreateFolder()
                            }
                        )
                        if (browserState.items.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_all)) },
                                leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                                onClick = {
                                    moreExpanded = false
                                    onSelectAll()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_options)) },
                            leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onOpenViewSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.refresh)) },
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                onRefresh()
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

private data class Breadcrumb(
    val label: String,
    val path: String
)

@Composable
private fun Breadcrumbs(
    path: String,
    rootPath: String,
    rootLabel: String,
    onPathClick: (String) -> Unit
) {
    val breadcrumbs = remember(path, rootPath, rootLabel) {
        buildBreadcrumbs(path, rootPath, rootLabel)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, breadcrumb ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = breadcrumb.label,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPathClick(breadcrumb.path) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (index == breadcrumbs.lastIndex) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildBreadcrumbs(
    path: String,
    rootPath: String,
    rootLabel: String
): List<Breadcrumb> {
    val root = Breadcrumb(rootLabel, rootPath)
    if (path == rootPath) return listOf(root)

    val relative = path.removePrefix(rootPath).trimStart('/', '\\')
    if (relative.isBlank()) return listOf(root)

    var currentPath = rootPath
    val children = relative
        .split('/', '\\')
        .filter(String::isNotBlank)
        .map { segment ->
            currentPath += File.separator + segment
            Breadcrumb(segment, currentPath)
        }
    return listOf(root) + children
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BrowserContent(
    browserState: BrowserUiState,
    viewStyle: ViewStyle,
    showFileExtensions: Boolean,
    storageAccessGranted: Boolean,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit,
    onOpenDirectory: (String) -> Unit,
    onRefresh: () -> Unit,
    onRequestStorageAccess: () -> Unit
) {
    when {
        browserState.isLoading -> LoadingBrowser()
        browserState.errorMessage != null -> BrowserError(
            message = browserState.errorMessage,
            storageAccessGranted = storageAccessGranted,
            onRefresh = onRefresh,
            onRequestStorageAccess = onRequestStorageAccess
        )
        browserState.items.isEmpty() -> EmptyFolder()
        isListFamily(viewStyle) -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(browserState.items, key = { it.path }) { file ->
                    when (viewStyle) {
                        ViewStyle.COMPACT_LIST -> CompactRowItem(
                            file,
                            showFileExtensions,
                            browserState.selectedPaths.isNotEmpty(),
                            file.path in browserState.selectedPaths,
                            onOpenDirectory,
                            onOpenFile,
                            onToggleSelection
                        )
                        ViewStyle.DETAILED_LIST -> DetailedRowItem(
                            file,
                            showFileExtensions,
                            browserState.selectedPaths.isNotEmpty(),
                            file.path in browserState.selectedPaths,
                            onOpenDirectory,
                            onOpenFile,
                            onToggleSelection
                        )
                        ViewStyle.COLUMNS -> NameRowItem(
                            file,
                            showFileExtensions,
                            browserState.selectedPaths.isNotEmpty(),
                            file.path in browserState.selectedPaths,
                            onOpenDirectory,
                            onOpenFile,
                            onToggleSelection
                        )
                        else -> FileListItem(
                            file = file,
                            showFileExtensions = showFileExtensions,
                            selectionMode = browserState.selectedPaths.isNotEmpty(),
                            isSelected = file.path in browserState.selectedPaths,
                            onOpenDirectory = onOpenDirectory,
                            onOpenFile = onOpenFile,
                            onToggleSelection = onToggleSelection
                        )
                    }
                    if (viewStyle == ViewStyle.LIST && file != browserState.items.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 84.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        else -> {
            val gridColumns = when (viewStyle) {
                ViewStyle.SMALL_ICON -> 88.dp
                ViewStyle.MEDIUM_ICON -> 116.dp
                ViewStyle.LARGE_ICON -> 148.dp
                ViewStyle.TILE -> 80.dp
                ViewStyle.CARD_VIEW, ViewStyle.MASONRY -> 180.dp
                ViewStyle.GALLERY -> 150.dp
                else -> 148.dp
            }
            val gridSpacing = if (viewStyle == ViewStyle.TILE) 4.dp else 12.dp
            val gridPadding = if (viewStyle == ViewStyle.TILE) {
                PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp)
            } else {
                PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
            }
            if (viewStyle == ViewStyle.MASONRY) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = gridPadding,
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalItemSpacing = gridSpacing
                ) {
                    items(browserState.items, key = { it.path }) { file ->
                        CardViewItem(
                            file,
                            showFileExtensions,
                            browserState.selectedPaths.isNotEmpty(),
                            file.path in browserState.selectedPaths,
                            onOpenDirectory,
                            onOpenFile,
                            onToggleSelection
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = gridPadding,
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing)
                ) {
                    items(browserState.items, key = { it.path }) { file ->
                        when (viewStyle) {
                            ViewStyle.SMALL_ICON -> IconGridItem(
                                file, showFileExtensions, 64.dp, showDetails = false,
                                browserState.selectedPaths.isNotEmpty(),
                                file.path in browserState.selectedPaths,
                                onOpenDirectory, onOpenFile, onToggleSelection
                            )
                            ViewStyle.MEDIUM_ICON -> IconGridItem(
                                file, showFileExtensions, 88.dp, showDetails = false,
                                browserState.selectedPaths.isNotEmpty(),
                                file.path in browserState.selectedPaths,
                                onOpenDirectory, onOpenFile, onToggleSelection
                            )
                            ViewStyle.LARGE_ICON -> IconGridItem(
                                file, showFileExtensions, 118.dp, showDetails = true,
                                browserState.selectedPaths.isNotEmpty(),
                                file.path in browserState.selectedPaths,
                                onOpenDirectory, onOpenFile, onToggleSelection
                            )
                            ViewStyle.TILE -> TileItem(
                                file, showFileExtensions,
                                browserState.selectedPaths.isNotEmpty(),
                                file.path in browserState.selectedPaths,
                                onOpenDirectory, onOpenFile, onToggleSelection
                            )
                            ViewStyle.GALLERY -> GalleryItem(
                                file, showFileExtensions,
                                browserState.selectedPaths.isNotEmpty(),
                                file.path in browserState.selectedPaths,
                                onOpenDirectory, onOpenFile, onToggleSelection
                            )
                            else -> FileGridItem(
                                file = file,
                                showFileExtensions = showFileExtensions,
                                selectionMode = browserState.selectedPaths.isNotEmpty(),
                                isSelected = file.path in browserState.selectedPaths,
                                onOpenDirectory = onOpenDirectory,
                                onOpenFile = onOpenFile,
                                onToggleSelection = onToggleSelection
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isListFamily(viewStyle: ViewStyle): Boolean = when (viewStyle) {
    ViewStyle.LIST, ViewStyle.COMPACT_LIST, ViewStyle.DETAILED_LIST, ViewStyle.COLUMNS -> true
    else -> false
}

@Composable
private fun LoadingBrowser() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(modifier = Modifier.width(160.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.browse_files),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BrowserError(
    message: String,
    storageAccessGranted: Boolean,
    onRefresh: () -> Unit,
    onRequestStorageAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (storageAccessGranted) {
            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(R.string.try_again))
            }
        } else {
            Button(onClick = onRequestStorageAccess) {
                Text(stringResource(R.string.grant_access))
            }
        }
    }
}

@Composable
private fun EmptyFolder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_folder),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_folder_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FileListItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelection(file.path)
                    } else if (file.isDirectory) {
                        onOpenDirectory(file.path)
                    } else {
                        onOpenFile(file)
                    }
                },
                onLongClick = {
                    onToggleSelection(file.path)
                }
            )
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
            .alpha(if (file.isHidden) 0.62f else 1f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileVisual(
            file = file,
            modifier = Modifier.size(52.dp),
            iconSize = 27.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayFileName(file, showFileExtensions),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            FileMetadata(file)
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        } else if (file.isDirectory) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FileGridItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Card(
        modifier = Modifier.combinedClickable(
            onClick = {
                if (selectionMode) {
                    onToggleSelection(file.path)
                } else if (file.isDirectory) {
                    onOpenDirectory(file.path)
                } else {
                    onOpenFile(file)
                }
            },
            onLongClick = { onToggleSelection(file.path) }
        ).alpha(if (file.isHidden) 0.62f else 1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            FileVisual(
                file = file,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                iconSize = 36.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = displayFileName(file, showFileExtensions),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            FileMetadata(file)
            if (isSelected) {
                Spacer(modifier = Modifier.height(6.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FileMetadata(file: ExplorerFile) {
    val details = if (file.isDirectory) {
        stringResource(R.string.folder)
    } else {
        listOfNotNull(
            formatBytes(file.sizeBytes),
            file.modifiedEpochMillis?.let(::formatModifiedDate)
        ).joinToString(" • ")
    }
    Text(
        text = details,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

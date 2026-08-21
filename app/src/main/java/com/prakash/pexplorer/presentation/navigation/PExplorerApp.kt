package com.prakash.pexplorer.presentation.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.openFile
import com.prakash.pexplorer.core.util.shareFiles
import com.prakash.pexplorer.domain.model.toExplorerFile
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.presentation.ExplorerViewModel
import com.prakash.pexplorer.presentation.StoredFileUi
import com.prakash.pexplorer.presentation.browser.FileBrowserScreen
import com.prakash.pexplorer.presentation.home.HomeScreen
import com.prakash.pexplorer.presentation.placeholder.UtilityScreen
import com.prakash.pexplorer.presentation.library.FavoritesLibraryScreen
import com.prakash.pexplorer.presentation.library.RecentLibraryScreen
import com.prakash.pexplorer.presentation.search.SearchScreen
import com.prakash.pexplorer.presentation.settings.SettingsScreen
import com.prakash.pexplorer.presentation.preview.PreviewScreen
import com.prakash.pexplorer.presentation.analyzer.DuplicateFinderScreen
import com.prakash.pexplorer.presentation.analyzer.LargeFilesScreen
import com.prakash.pexplorer.presentation.analyzer.StorageAnalyzerScreen
import com.prakash.pexplorer.presentation.components.TransferProgressDialog
import com.prakash.pexplorer.presentation.network.NetworkScreen

@Composable
fun PExplorerApp(
    viewModel: ExplorerViewModel,
    onRequestStorageAccess: () -> Unit,
    onPickNetworkFile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var createFolderRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val openExplorerFile: (ExplorerFile) -> Unit = { file ->
        if (file.kind == FileKind.IMAGE || file.kind == FileKind.TEXT) {
            viewModel.openPreview(file)
            navController.navigate(PREVIEW_ROUTE)
        } else {
            openFile(context, file)?.let(viewModel::showMessage)
                ?: viewModel.recordRecent(file)
        }
    }
    val openStoredFile: (StoredFileUi) -> Unit = { item ->
        if (!item.isAvailable) {
            viewModel.showMessage(context.getString(R.string.unavailable))
        } else {
            val file = item.reference.toExplorerFile()
            if (file.isDirectory) {
                viewModel.openDirectory(file.path)
                navController.navigateTopLevel(TopLevelDestination.FILES)
            } else {
                openExplorerFile(file)
            }
        }
    }

    val onTopLevelSelected: (TopLevelDestination) -> Unit = { destination ->
        if (destination == TopLevelDestination.FILES) {
            viewModel.openRoot()
        }
        navController.navigateTopLevel(destination)
    }
    val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
    val swipeModifier = if (currentRoute in topLevelRoutes) {
        Modifier.pointerInput(currentRoute) {
            var totalDrag = 0f
            val threshold = 80.dp.toPx()
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onDragEnd = {
                    val index = TopLevelDestination.entries.indexOfFirst { it.route == currentRoute }
                    when {
                        index < 0 -> Unit
                        totalDrag <= -threshold && index < TopLevelDestination.entries.lastIndex ->
                            onTopLevelSelected(TopLevelDestination.entries[index + 1])
                        totalDrag >= threshold && index > 0 ->
                            onTopLevelSelected(TopLevelDestination.entries[index - 1])
                    }
                },
                onHorizontalDrag = { _, amount -> totalDrag += amount }
            )
        }
    } else {
        Modifier
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onDestinationSelected = onTopLevelSelected
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier
                .padding(paddingValues)
                .then(swipeModifier)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    state = uiState,
                    onOpenFiles = {
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    },
                    onOpenSearch = {
                        viewModel.clearSearchCategory()
                        navController.navigate(SEARCH_ROUTE)
                    },
                    onOpenCategory = { category ->
                        viewModel.openCategory(category)
                        navController.navigate(SEARCH_ROUTE)
                    },
                    onCreateFolder = {
                        createFolderRequested = true
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    },
                    onOpenStorage = { path ->
                        viewModel.openFolderInNewTab(path)
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    },
                    onRequestStorageAccess = onRequestStorageAccess,
                    onOpenUtility = { destination ->
                        navController.navigate(destination.route)
                    }
                )
            }
            composable(TopLevelDestination.FILES.route) {
                FileBrowserScreen(
                    browserState = uiState.browser,
                    rootPath = uiState.browserRootPath,
                    rootLabel = uiState.storage
                        .firstOrNull { it.path == uiState.browserRootPath }
                        ?.label
                        ?: stringResource(R.string.internal_storage),
                    tabs = uiState.tabs,
                    activeTabId = uiState.activeTabId,
                    onSwitchTab = viewModel::switchTab,
                    onCloseTab = viewModel::closeTab,
                    onNewTab = viewModel::openNewTab,
                    viewStyle = uiState.viewStyle,
                    sortOrder = uiState.preferences.sortOrder,
                    foldersFirst = uiState.preferences.foldersFirst,
                    showFileExtensions = uiState.preferences.showFileExtensions,
                    confirmBeforeDelete = uiState.preferences.confirmBeforeDelete,
                    storageAccessGranted = uiState.storageAccessGranted,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateUp = viewModel::goUp,
                    onOpenDirectory = viewModel::openDirectory,
                    onOpenFile = { file ->
                        openExplorerFile(file)
                    },
                    onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
                    onRefresh = viewModel::refresh,
                    onRequestStorageAccess = onRequestStorageAccess,
                    onToggleSelection = viewModel::toggleSelection,
                    onSelectAll = viewModel::selectAll,
                    onClearSelection = viewModel::clearSelection,
                    onApplyViewSettings = viewModel::setViewSettings,
                    onCreateFolder = viewModel::createFolder,
                    onRename = viewModel::rename,
                    onDelete = viewModel::delete,
                    onCopy = viewModel::copy,
                    onMove = viewModel::move,
                    onCompress = { paths, archiveName ->
                        viewModel.compress(paths, uiState.browser.path, archiveName)
                    },
                    onExtract = { path, extractToNewFolder ->
                        viewModel.extract(path, uiState.browser.path, extractToNewFolder)
                    },
                    onShare = { files ->
                        shareFiles(context, files)?.let(viewModel::showMessage)
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                    isFavorite = viewModel::isFavorite,
                    onShowProperties = viewModel::loadProperties,
                    transfer = uiState.transfer,
                    properties = uiState.properties,
                    onCancelTransfer = viewModel::cancelTransfer,
                    onDismissTransfer = viewModel::dismissTransfer,
                    onDismissProperties = viewModel::clearProperties,
                    initiallyShowCreateFolder = createFolderRequested,
                    onCreateFolderRequestConsumed = { createFolderRequested = false }
                )
            }
            composable(SEARCH_ROUTE) {
                SearchScreen(
                    state = uiState.search,
                    recentSearches = uiState.preferences.searchHistory,
                    showFileExtensions = uiState.preferences.showFileExtensions,
                    onQueryChanged = viewModel::updateSearchQuery,
                    onUseRecentSearch = viewModel::updateSearchQuery,
                    onClearHistory = viewModel::clearSearchHistory,
                    onOpenFile = { file ->
                        openExplorerFile(file)
                    },
                    onOpenDirectory = { path ->
                        viewModel.openDirectory(path)
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    },
                    onBack = {
                        viewModel.clearSearchCategory()
                        navController.popBackStack()
                    }
                )
            }
            composable(PREVIEW_ROUTE) {
                uiState.preview?.let { preview ->
                    PreviewScreen(
                        state = preview,
                        onBack = {
                            viewModel.clearPreview()
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(TopLevelDestination.RECENT.route) {
                RecentLibraryScreen(
                    items = uiState.recentItems,
                    showFileExtensions = uiState.preferences.showFileExtensions,
                    onOpen = openStoredFile,
                    onRemove = viewModel::removeRecent,
                    onClearAll = viewModel::clearRecent,
                    onOpenFiles = {
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    }
                )
            }
            composable(TopLevelDestination.FAVORITES.route) {
                FavoritesLibraryScreen(
                    items = uiState.favoriteItems,
                    showFileExtensions = uiState.preferences.showFileExtensions,
                    onOpen = openStoredFile,
                    onRemove = viewModel::removeFavorite,
                    onOpenFiles = {
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    }
                )
            }
            composable(LARGE_FILES_ROUTE) {
                LargeFilesScreen(
                    state = uiState.largeFiles,
                    showFileExtensions = uiState.preferences.showFileExtensions,
                    onScan = viewModel::scanLargeFiles,
                    onOpenFile = openExplorerFile,
                    onDelete = viewModel::delete,
                    onShare = { files ->
                        shareFiles(context, files)?.let(viewModel::showMessage)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(DUPLICATES_ROUTE) {
                DuplicateFinderScreen(
                    state = uiState.duplicates,
                    showFileExtensions = uiState.preferences.showFileExtensions,
                    onScan = viewModel::scanDuplicates,
                    onOpenFile = openExplorerFile,
                    onDelete = viewModel::delete,
                    onShare = { files ->
                        shareFiles(context, files)?.let(viewModel::showMessage)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            UtilityDestination.entries.forEach { destination ->
                composable(destination.route) {
                    if (destination == UtilityDestination.P_HUB) {
                        NetworkScreen(
                            state = uiState.network,
                            onDiscover = viewModel::discoverNetwork,
                            onRequestChallenge = viewModel::requestNetworkChallenge,
                            onConnect = viewModel::connectNetwork,
                            onOpenDirectory = viewModel::openNetworkDirectory,
                            onGoUp = viewModel::goUpNetwork,
                            onDownload = viewModel::downloadNetworkFile,
                            onUpload = onPickNetworkFile,
                            onDisconnect = viewModel::disconnectNetwork,
                            onBack = { navController.popBackStack() }
                        )
                    } else if (destination == UtilityDestination.ANALYZER) {
                        StorageAnalyzerScreen(
                            state = uiState.analyzer,
                            showFileExtensions = uiState.preferences.showFileExtensions,
                            onScan = viewModel::scanStorageAnalysis,
                            onOpenLargeFiles = { navController.navigate(LARGE_FILES_ROUTE) },
                            onOpenDuplicates = { navController.navigate(DUPLICATES_ROUTE) },
                            onOpenFile = openExplorerFile,
                            onDelete = viewModel::delete,
                            onShare = { files ->
                                shareFiles(context, files)?.let(viewModel::showMessage)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    } else if (destination == UtilityDestination.SETTINGS) {
                        SettingsScreen(
                            preferences = uiState.preferences,
                            storage = uiState.storage,
                            onBack = { navController.popBackStack() },
                            onThemeModeChanged = viewModel::setThemeMode,
                            onViewModeChanged = viewModel::setViewStyle,
                            onSortOrderChanged = viewModel::setSortOrder,
                            onFoldersFirstChanged = viewModel::setFoldersFirst,
                            onShowHiddenChanged = viewModel::setShowHiddenFiles,
                            onShowExtensionsChanged = viewModel::setShowFileExtensions,
                            onConfirmDeleteChanged = viewModel::setConfirmBeforeDelete,
                            onConfirmOverwriteChanged = viewModel::setConfirmBeforeOverwrite,
                            onRememberLastFolderChanged = viewModel::setRememberLastFolder,
                            onRecentItemsChanged = viewModel::setRecentItemsEnabled,
                            onClearCache = viewModel::clearCache
                        )
                    } else {
                        UtilityScreen(
                            destination = destination,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
    if (currentRoute != TopLevelDestination.FILES.route) {
        uiState.transfer?.let { transfer ->
            TransferProgressDialog(
                state = transfer,
                onCancel = viewModel::cancelTransfer,
                onDismiss = viewModel::dismissTransfer
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(
        destination.route,
        navOptions {
            popUpTo(TopLevelDestination.HOME.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    )
}

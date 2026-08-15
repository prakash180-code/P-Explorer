package com.prakash.pexplorer.presentation.navigation

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.prakash.pexplorer.presentation.ExplorerViewModel
import com.prakash.pexplorer.presentation.browser.FileBrowserScreen
import com.prakash.pexplorer.presentation.home.HomeScreen
import com.prakash.pexplorer.presentation.placeholder.FavoritesScreen
import com.prakash.pexplorer.presentation.placeholder.RecentScreen
import com.prakash.pexplorer.presentation.placeholder.UtilityScreen

@Composable
fun PExplorerApp(
    viewModel: ExplorerViewModel,
    onRequestStorageAccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var createFolderRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

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
                onDestinationSelected = { destination ->
                    if (destination == TopLevelDestination.FILES) {
                        viewModel.openRoot()
                    }
                    navController.navigateTopLevel(destination)
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    state = uiState,
                    onOpenFiles = {
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    },
                    onCreateFolder = {
                        createFolderRequested = true
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    },
                    onOpenStorage = { path ->
                        viewModel.openDirectory(path)
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
                    viewMode = uiState.viewMode,
                    storageAccessGranted = uiState.storageAccessGranted,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateUp = viewModel::goUp,
                    onOpenDirectory = viewModel::openDirectory,
                    onOpenFile = { file ->
                        openFile(context, file)?.let(viewModel::showMessage)
                    },
                    onViewModeChanged = viewModel::setViewMode,
                    onRefresh = viewModel::refresh,
                    onRequestStorageAccess = onRequestStorageAccess,
                    onToggleSelection = viewModel::toggleSelection,
                    onSelectAll = viewModel::selectAll,
                    onClearSelection = viewModel::clearSelection,
                    onCreateFolder = viewModel::createFolder,
                    onRename = viewModel::rename,
                    onDelete = viewModel::delete,
                    onCopy = viewModel::copy,
                    onMove = viewModel::move,
                    onShare = { files ->
                        shareFiles(context, files)?.let(viewModel::showMessage)
                    },
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
            composable(TopLevelDestination.RECENT.route) {
                RecentScreen(
                    onOpenFiles = {
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    }
                )
            }
            composable(TopLevelDestination.FAVORITES.route) {
                FavoritesScreen(
                    onOpenFiles = {
                        viewModel.openRoot()
                        navController.navigateTopLevel(TopLevelDestination.FILES)
                    }
                )
            }
            UtilityDestination.entries.forEach { destination ->
                composable(destination.route) {
                    UtilityScreen(
                        destination = destination,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
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

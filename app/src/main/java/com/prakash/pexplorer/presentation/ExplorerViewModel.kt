package com.prakash.pexplorer.presentation

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pexplorer.R
import com.prakash.pexplorer.data.filesystem.LocalFileSystemProvider
import com.prakash.pexplorer.data.network.NetworkFileProvider
import com.prakash.pexplorer.data.network.PHubNetworkProvider
import com.prakash.pexplorer.data.preferences.MetadataStore
import com.prakash.pexplorer.data.repository.FileRepository
import com.prakash.pexplorer.data.repository.MetadataRepository
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.ExplorerPreferences
import com.prakash.pexplorer.domain.model.ExplorerTab
import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.FileReference
import com.prakash.pexplorer.domain.model.DuplicateGroup
import com.prakash.pexplorer.domain.model.NetworkDevice
import com.prakash.pexplorer.domain.model.NetworkFileEntry
import com.prakash.pexplorer.domain.model.NetworkTransferProgress
import com.prakash.pexplorer.domain.model.SearchUiState
import com.prakash.pexplorer.domain.model.ScanProgress
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.StorageAnalysis
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.ThemeMode
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.model.ViewStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class BrowserUiState(
    val path: String,
    val items: List<ExplorerFile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedPaths: Set<String> = emptySet()
)

data class TransferUiState(
    val operation: FileOperation,
    val currentName: String? = null,
    val completedItems: Int = 0,
    val totalItems: Int,
    val isRunning: Boolean = true,
    val errorMessage: String? = null
) {
    val fraction: Float
        get() = if (totalItems > 0) {
            (completedItems.toFloat() / totalItems).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class PropertiesUiState(
    val path: String,
    val isLoading: Boolean = true,
    val properties: FileProperties? = null,
    val errorMessage: String? = null
)

data class StoredFileUi(
    val reference: FileReference,
    val isAvailable: Boolean
)

data class ScanUiState(
    val isScanning: Boolean = false,
    val scannedFiles: Int = 0,
    val currentPath: String? = null,
    val errorMessage: String? = null
)

data class AnalyzerUiState(
    val scan: ScanUiState = ScanUiState(),
    val analysis: StorageAnalysis? = null
)

data class LargeFilesUiState(
    val minimumBytes: Long = 100L * 1024L * 1024L,
    val scan: ScanUiState = ScanUiState(),
    val files: List<ExplorerFile> = emptyList()
)

data class DuplicateUiState(
    val scan: ScanUiState = ScanUiState(),
    val groups: List<DuplicateGroup> = emptyList()
)

data class NetworkUiState(
    val isDiscovering: Boolean = false,
    val devices: List<NetworkDevice> = emptyList(),
    val selectedDevice: NetworkDevice? = null,
    val challengeCodes: List<String> = emptyList(),
    val isConnecting: Boolean = false,
    val connected: Boolean = false,
    val roots: List<NetworkFileEntry> = emptyList(),
    val files: List<NetworkFileEntry> = emptyList(),
    val currentPath: String? = null,
    val pathStack: List<String> = emptyList(),
    val download: NetworkTransferProgress? = null,
    val errorMessage: String? = null
)

data class ExplorerUiState(
    val storage: List<StorageInfo> = emptyList(),
    val storageLoading: Boolean = true,
    val storageError: String? = null,
    val storageAccessGranted: Boolean = false,
    val viewStyle: ViewStyle = ViewStyle.LIST,
    val browserRootPath: String,
    val browser: BrowserUiState,
    val transfer: TransferUiState? = null,
    val properties: PropertiesUiState? = null,
    val preferences: ExplorerPreferences = ExplorerPreferences(),
    val favoriteItems: List<StoredFileUi> = emptyList(),
    val recentItems: List<StoredFileUi> = emptyList(),
    val search: SearchUiState = SearchUiState(),
    val preview: PreviewUiState? = null,
    val analyzer: AnalyzerUiState = AnalyzerUiState(),
    val largeFiles: LargeFilesUiState = LargeFilesUiState(),
    val duplicates: DuplicateUiState = DuplicateUiState(),
    val network: NetworkUiState = NetworkUiState(),
    val tabs: List<ExplorerTab> = emptyList(),
    val activeTabId: String = ""
)

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(LocalFileSystemProvider(application))
    private val metadataRepository = MetadataRepository(MetadataStore(application))
    private val networkProvider: NetworkFileProvider = PHubNetworkProvider(application)
    private val initialTabId = UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(
        ExplorerUiState(
            browserRootPath = repository.primaryStoragePath,
            browser = BrowserUiState(path = repository.primaryStoragePath),
            tabs = listOf(
                ExplorerTab(
                    id = initialTabId,
                    title = getApplication<Application>().getString(R.string.internal_storage),
                    path = repository.primaryStoragePath,
                    rootPath = repository.primaryStoragePath
                )
            ),
            activeTabId = initialTabId
        )
    )
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var directoryJob: Job? = null
    private var operationJob: Job? = null
    private var propertiesJob: Job? = null
    private var searchJob: Job? = null
    private var storedItemsJob: Job? = null
    private var previewJob: Job? = null
    private var analyzerJob: Job? = null
    private var largeFilesJob: Job? = null
    private var duplicatesJob: Job? = null
    private var networkJob: Job? = null
    private var preferencesInitialized = false

    init {
        observePreferences()
        refresh()
    }

    fun refresh() {
        val accessGranted = repository.hasStorageAccess()
        _uiState.update {
            it.copy(
                storageAccessGranted = accessGranted,
                browser = it.browser.copy(
                    isLoading = false,
                    errorMessage = if (accessGranted) null else permissionMessage(),
                    selectedPaths = emptySet()
                )
            )
        }
        loadStorage()
        if (accessGranted) {
            openDirectory(_uiState.value.browser.path)
        }
    }

    fun openRoot() {
        openDirectory(_uiState.value.browserRootPath)
    }

    fun openFolderInNewTab(path: String) {
        if (!repository.isPathInsideStorage(path)) {
            _uiState.update {
                it.copy(browser = it.browser.copy(isLoading = false, errorMessage = storageUnavailableMessage()))
            }
            return
        }
        val rootPath = repository.storageRootPathFor(path) ?: repository.primaryStoragePath
        val tab = ExplorerTab(
            id = UUID.randomUUID().toString(),
            title = tabTitle(path, rootPath),
            path = path,
            rootPath = rootPath
        )
        _uiState.update {
            it.copy(
                tabs = it.tabs + tab,
                activeTabId = tab.id,
                browserRootPath = rootPath,
                browser = BrowserUiState(path = path, isLoading = true)
            )
        }
        openDirectory(path)
    }

    fun openNewTab() {
        openFolderInNewTab(repository.primaryStoragePath)
    }

    fun switchTab(tabId: String) {
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.id == _uiState.value.activeTabId) return
        directoryJob?.cancel()
        _uiState.update {
            it.copy(
                activeTabId = tab.id,
                browserRootPath = tab.rootPath,
                browser = BrowserUiState(path = tab.path, isLoading = true)
            )
        }
        openDirectory(tab.path)
    }

    fun closeTab(tabId: String) {
        val state = _uiState.value
        if (state.tabs.size <= 1) {
            openRoot()
            return
        }
        val tabIndex = state.tabs.indexOfFirst { it.id == tabId }
        if (tabIndex < 0) return
        val closingActive = tabId == state.activeTabId
        val remaining = state.tabs.filterNot { it.id == tabId }
        val nextTab = if (closingActive) {
            remaining.getOrNull(tabIndex.coerceAtMost(remaining.lastIndex)) ?: remaining.last()
        } else {
            remaining.firstOrNull { it.id == state.activeTabId } ?: remaining.first()
        }
        _uiState.update {
            if (closingActive) {
                it.copy(
                    tabs = remaining,
                    activeTabId = nextTab.id,
                    browserRootPath = nextTab.rootPath,
                    browser = BrowserUiState(path = nextTab.path, isLoading = true)
                )
            } else {
                it.copy(tabs = remaining)
            }
        }
        if (closingActive) openDirectory(nextTab.path)
    }

    fun openDirectory(path: String) {
        if (!repository.isPathInsideStorage(path)) {
            _uiState.update {
                it.copy(browser = it.browser.copy(isLoading = false, errorMessage = storageUnavailableMessage()))
            }
            return
        }

        directoryJob?.cancel()
        val browserRootPath = repository.storageRootPathFor(path) ?: _uiState.value.browserRootPath
        val preferences = _uiState.value.preferences
        val activeTabId = _uiState.value.activeTabId
        val updatedTab = _uiState.value.tabs
            .firstOrNull { it.id == activeTabId }
            ?.copy(
                title = tabTitle(path, browserRootPath),
                path = path,
                rootPath = browserRootPath
            )
        _uiState.update {
            it.copy(
                browserRootPath = browserRootPath,
                tabs = if (updatedTab == null) it.tabs else it.tabs.map { tab ->
                    if (tab.id == activeTabId) updatedTab else tab
                },
                browser = it.browser.copy(
                    path = path,
                    items = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                    selectedPaths = emptySet()
                )
            )
        }

        if (preferencesInitialized && preferences.rememberLastFolder) {
            viewModelScope.launch { metadataRepository.setLastFolder(path) }
        }

        if (!repository.hasStorageAccess()) {
            _uiState.update {
                it.copy(browser = it.browser.copy(isLoading = false, errorMessage = permissionMessage()))
            }
            return
        }

        directoryJob = viewModelScope.launch {
            repository.listDirectory(
                path = path,
                showHidden = preferences.showHiddenFiles,
                sortOrder = preferences.sortOrder,
                foldersFirst = preferences.foldersFirst
            )
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(
                            storageAccessGranted = true,
                            browser = it.browser.copy(
                                items = files,
                                isLoading = false,
                                errorMessage = null
                            )
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            browser = it.browser.copy(
                                isLoading = false,
                                errorMessage = friendlyError(error)
                            )
                        )
                    }
                }
        }
    }

    fun goUp(): Boolean {
        val currentPath = _uiState.value.browser.path
        val parent = File(currentPath).parentFile?.absolutePath ?: return false
        if (parent == currentPath || !repository.isPathInsideStorage(parent)) return false
        openDirectory(parent)
        return true
    }

    fun setViewStyle(viewStyle: ViewStyle) {
        _uiState.update { it.copy(viewStyle = viewStyle) }
        viewModelScope.launch { metadataRepository.setViewMode(viewStyle) }
    }

    fun setViewSettings(
        viewStyle: ViewStyle,
        sortOrder: SortOrder,
        foldersFirst: Boolean
    ) {
        _uiState.update { it.copy(viewStyle = viewStyle) }
        viewModelScope.launch {
            metadataRepository.setViewMode(viewStyle)
            metadataRepository.setSortOrder(sortOrder)
            metadataRepository.setFoldersFirst(foldersFirst)
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch { metadataRepository.setSortOrder(sortOrder) }
    }

    fun setFoldersFirst(enabled: Boolean) {
        viewModelScope.launch { metadataRepository.setFoldersFirst(enabled) }
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        viewModelScope.launch { metadataRepository.setShowHiddenFiles(enabled) }
    }

    fun setShowFileExtensions(enabled: Boolean) {
        viewModelScope.launch { metadataRepository.setShowFileExtensions(enabled) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { metadataRepository.setThemeMode(themeMode) }
    }

    fun setConfirmBeforeDelete(enabled: Boolean) {
        viewModelScope.launch { metadataRepository.setConfirmBeforeDelete(enabled) }
    }

    fun setConfirmBeforeOverwrite(enabled: Boolean) {
        viewModelScope.launch { metadataRepository.setConfirmBeforeOverwrite(enabled) }
    }

    fun setRememberLastFolder(enabled: Boolean) {
        viewModelScope.launch { metadataRepository.setRememberLastFolder(enabled) }
    }

    fun clearCache() {
        repository.invalidateSearchIndex()
        emitMessage(R.string.cache_cleared)
    }

    fun isFavorite(path: String): Boolean =
        _uiState.value.preferences.favorites.any { it.path == path }

    fun toggleFavorite(file: ExplorerFile) {
        viewModelScope.launch { metadataRepository.toggleFavorite(file.toReference()) }
    }

    fun removeFavorite(path: String) {
        viewModelScope.launch { metadataRepository.removeFavorite(path) }
    }

    fun recordRecent(file: ExplorerFile) {
        viewModelScope.launch {
            metadataRepository.recordRecent(file.toReference(System.currentTimeMillis()))
        }
    }

    fun removeRecent(path: String) {
        viewModelScope.launch { metadataRepository.removeRecent(path) }
    }

    fun clearRecent() {
        viewModelScope.launch { metadataRepository.clearRecent() }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { metadataRepository.clearSearchHistory() }
    }

    fun openPreview(file: ExplorerFile) {
        previewJob?.cancel()
        _uiState.update {
            it.copy(
                preview = PreviewUiState(
                    file = file,
                    isLoading = file.kind == com.prakash.pexplorer.domain.model.FileKind.TEXT
                )
            )
        }
        if (file.kind != com.prakash.pexplorer.domain.model.FileKind.TEXT) return
        previewJob = viewModelScope.launch {
            repository.readText(file.path, MAX_TEXT_PREVIEW_BYTES)
                .onSuccess { content ->
                    _uiState.update {
                        it.copy(
                            preview = it.preview?.copy(
                                text = content.value,
                                isLoading = false,
                                isTruncated = content.isTruncated
                            )
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            preview = it.preview?.copy(
                                isLoading = false,
                                errorMessage = friendlyError(error)
                            )
                        )
                    }
                }
        }
    }

    fun clearPreview() {
        previewJob?.cancel()
        previewJob = null
        _uiState.update { it.copy(preview = null) }
    }

    fun scanStorageAnalysis() {
        analyzerJob?.cancel()
        _uiState.update {
            it.copy(
                analyzer = it.analyzer.copy(
                    scan = ScanUiState(isScanning = true),
                    analysis = null
                )
            )
        }
        analyzerJob = viewModelScope.launch {
            repository.analyzeStorage(::updateAnalyzerProgress)
                .onSuccess { analysis ->
                    _uiState.update {
                        it.copy(
                            analyzer = it.analyzer.copy(
                                scan = ScanUiState(),
                                analysis = analysis
                            )
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            analyzer = it.analyzer.copy(
                                scan = ScanUiState(errorMessage = friendlyError(error)),
                                analysis = null
                            )
                        )
                    }
                }
        }
    }

    fun scanLargeFiles(minimumBytes: Long = _uiState.value.largeFiles.minimumBytes) {
        largeFilesJob?.cancel()
        _uiState.update {
            it.copy(
                largeFiles = it.largeFiles.copy(
                    minimumBytes = minimumBytes,
                    scan = ScanUiState(isScanning = true),
                    files = emptyList()
                )
            )
        }
        largeFilesJob = viewModelScope.launch {
            repository.findLargeFiles(
                minimumBytes = minimumBytes,
                showHidden = _uiState.value.preferences.showHiddenFiles,
                onProgress = ::updateLargeFilesProgress
            ).onSuccess { files ->
                _uiState.update {
                    it.copy(
                        largeFiles = it.largeFiles.copy(scan = ScanUiState(), files = files)
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        largeFiles = it.largeFiles.copy(
                            scan = ScanUiState(errorMessage = friendlyError(error)),
                            files = emptyList()
                        )
                    )
                }
            }
        }
    }

    fun scanDuplicates() {
        duplicatesJob?.cancel()
        _uiState.update {
            it.copy(duplicates = it.duplicates.copy(scan = ScanUiState(isScanning = true), groups = emptyList()))
        }
        duplicatesJob = viewModelScope.launch {
            repository.findDuplicates(
                showHidden = _uiState.value.preferences.showHiddenFiles,
                onProgress = ::updateDuplicateProgress
            ).onSuccess { groups ->
                _uiState.update {
                    it.copy(duplicates = it.duplicates.copy(scan = ScanUiState(), groups = groups))
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update {
                    it.copy(
                        duplicates = it.duplicates.copy(
                            scan = ScanUiState(errorMessage = friendlyError(error)),
                            groups = emptyList()
                        )
                    )
                }
            }
        }
    }

    fun discoverNetwork() {
        networkJob?.cancel()
        _uiState.update {
            it.copy(network = it.network.copy(isDiscovering = true, errorMessage = null, devices = emptyList()))
        }
        networkJob = viewModelScope.launch {
            networkProvider.discover()
                .onSuccess { devices ->
                    _uiState.update {
                        it.copy(network = it.network.copy(isDiscovering = false, devices = devices))
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(network = it.network.copy(isDiscovering = false, errorMessage = networkError(error)))
                    }
                }
        }
    }

    fun requestNetworkChallenge(device: NetworkDevice) {
        networkJob?.cancel()
        _uiState.update {
            it.copy(
                network = it.network.copy(
                    selectedDevice = device,
                    challengeCodes = emptyList(),
                    isConnecting = true,
                    errorMessage = null
                )
            )
        }
        networkJob = viewModelScope.launch {
            networkProvider.requestChallenge(device)
                .onSuccess { codes ->
                    _uiState.update {
                        it.copy(network = it.network.copy(isConnecting = false, challengeCodes = codes))
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(network = it.network.copy(isConnecting = false, errorMessage = networkError(error)))
                    }
                }
        }
    }

    fun connectNetwork(authCode: String) {
        val device = _uiState.value.network.selectedDevice ?: return
        networkJob?.cancel()
        _uiState.update {
            it.copy(network = it.network.copy(isConnecting = true, errorMessage = null))
        }
        networkJob = viewModelScope.launch {
            networkProvider.connect(device, authCode)
                .onSuccess { roots ->
                    _uiState.update {
                        it.copy(
                            network = it.network.copy(
                                isConnecting = false,
                                connected = true,
                                challengeCodes = emptyList(),
                                roots = roots,
                                files = emptyList(),
                                currentPath = null,
                                pathStack = emptyList(),
                                errorMessage = null
                            )
                        )
                    }
                    emitMessage(R.string.network_connected)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(network = it.network.copy(isConnecting = false, errorMessage = networkError(error)))
                    }
                }
        }
    }

    fun openNetworkDirectory(path: String) {
        networkJob?.cancel()
        val previousPath = _uiState.value.network.currentPath
        _uiState.update {
            it.copy(
                network = it.network.copy(
                    isConnecting = true,
                    currentPath = path,
                    pathStack = if (previousPath == null) it.network.pathStack else it.network.pathStack + previousPath,
                    errorMessage = null
                )
            )
        }
        networkJob = viewModelScope.launch {
            networkProvider.list(path)
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(network = it.network.copy(isConnecting = false, files = files))
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(network = it.network.copy(isConnecting = false, errorMessage = networkError(error)))
                    }
                }
        }
    }

    fun goUpNetwork() {
        val state = _uiState.value.network
        val parent = state.pathStack.lastOrNull()
        if (parent == null) {
            _uiState.update { it.copy(network = it.network.copy(currentPath = null, files = emptyList())) }
        } else {
            _uiState.update {
                it.copy(
                    network = it.network.copy(
                        pathStack = state.pathStack.dropLast(1),
                        currentPath = parent,
                        isConnecting = true,
                        errorMessage = null
                    )
                )
            }
            networkJob?.cancel()
            networkJob = viewModelScope.launch {
                networkProvider.list(parent)
                    .onSuccess { files ->
                        _uiState.update {
                            it.copy(network = it.network.copy(isConnecting = false, files = files))
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        _uiState.update {
                            it.copy(network = it.network.copy(isConnecting = false, errorMessage = networkError(error)))
                        }
                    }
            }
        }
    }

    fun downloadNetworkFile(entry: NetworkFileEntry) {
        networkJob?.cancel()
        networkJob = viewModelScope.launch {
            networkProvider.download(entry) { progress ->
                _uiState.update { it.copy(network = it.network.copy(download = progress)) }
            }.onSuccess {
                _uiState.update { it.copy(network = it.network.copy(download = null)) }
                emitMessage(R.string.network_downloaded)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update { it.copy(network = it.network.copy(download = null, errorMessage = networkError(error))) }
            }
        }
    }

    fun uploadNetworkFile(uri: Uri) {
        networkJob?.cancel()
        networkJob = viewModelScope.launch {
            val application = getApplication<Application>()
            val resolver = application.contentResolver
            val fileName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                ?: "upload_${System.currentTimeMillis()}"
            val temporaryFile = File(application.cacheDir, "network-upload-${System.currentTimeMillis()}")
            try {
                withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { input ->
                        temporaryFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not read the selected file")
                }
                val uploadFile = File(temporaryFile.parentFile, fileName).also {
                    temporaryFile.renameTo(it)
                }
                networkProvider.upload(uploadFile, _uiState.value.network.currentPath.orEmpty()) { progress ->
                    _uiState.update { it.copy(network = it.network.copy(download = progress)) }
                }.onSuccess {
                    _uiState.update { it.copy(network = it.network.copy(download = null)) }
                    emitMessage(R.string.network_uploaded)
                    val currentPath = _uiState.value.network.currentPath
                    if (currentPath != null) {
                        networkProvider.list(currentPath).onSuccess { files ->
                            _uiState.update { it.copy(network = it.network.copy(files = files)) }
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(network = it.network.copy(download = null, errorMessage = networkError(error)))
                    }
                }
                uploadFile.delete()
            } catch (error: Throwable) {
                if (error !is CancellationException) {
                    _uiState.update {
                        it.copy(network = it.network.copy(download = null, errorMessage = networkError(error)))
                    }
                }
                temporaryFile.delete()
            }
        }
    }

    fun disconnectNetwork() {
        networkJob?.cancel()
        networkProvider.disconnect()
        _uiState.update { it.copy(network = NetworkUiState()) }
    }

    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                search = it.search.copy(
                    query = query,
                    results = if (query.isBlank()) emptyList() else it.search.results,
                    hasSearched = if (query.isBlank()) false else it.search.hasSearched,
                    errorMessage = null
                )
            )
        }
        if (query.isBlank()) return

        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(search = it.search.copy(isSearching = true, errorMessage = null)) }
            repository.search(query, _uiState.value.preferences.showHiddenFiles)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            search = it.search.copy(
                                results = results,
                                isSearching = false,
                                hasSearched = true,
                                errorMessage = null
                            )
                        )
                    }
                    metadataRepository.recordSearch(query)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            search = it.search.copy(
                                isSearching = false,
                                hasSearched = true,
                                errorMessage = friendlyError(error)
                            )
                        )
                    }
                }
        }
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val selected = state.browser.selectedPaths.toMutableSet()
            if (!selected.add(path)) selected.remove(path)
            state.copy(browser = state.browser.copy(selectedPaths = selected))
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(
                browser = state.browser.copy(
                    selectedPaths = state.browser.items.mapTo(mutableSetOf(), ExplorerFile::path)
                )
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(browser = it.browser.copy(selectedPaths = emptySet())) }
    }

    fun createFolder(name: String) {
        val parentPath = _uiState.value.browser.path
        viewModelScope.launch {
            repository.createDirectory(parentPath, name.trim())
                .onSuccess {
                    emitMessage(R.string.folder_created)
                    openDirectory(parentPath)
                    refreshStoredItems(_uiState.value.preferences)
                }
                .onFailure(::emitOperationError)
        }
    }

    fun rename(path: String, newName: String) {
        val parentPath = _uiState.value.browser.path
        viewModelScope.launch {
            repository.rename(path, newName.trim())
                .onSuccess {
                    emitMessage(R.string.renamed_successfully)
                    openDirectory(parentPath)
                    refreshStoredItems(_uiState.value.preferences)
                }
                .onFailure(::emitOperationError)
        }
    }

    fun delete(paths: List<String>) {
        startTransfer(FileOperation.DELETE, paths, destinationDirectory = null)
    }

    fun copy(paths: List<String>, destinationDirectory: String) {
        startTransfer(FileOperation.COPY, paths, destinationDirectory)
    }

    fun move(paths: List<String>, destinationDirectory: String) {
        startTransfer(FileOperation.MOVE, paths, destinationDirectory)
    }

    fun compress(paths: List<String>, destinationDirectory: String, archiveName: String) {
        startArchive(FileOperation.COMPRESS, paths, destinationDirectory, archiveName, false)
    }

    fun extract(path: String, destinationDirectory: String, extractToNewFolder: Boolean) {
        startArchive(FileOperation.EXTRACT, listOf(path), destinationDirectory, null, extractToNewFolder)
    }

    fun cancelTransfer() {
        operationJob?.cancel()
        operationJob = null
        _uiState.update { it.copy(transfer = null) }
        emitMessage(R.string.operation_cancelled)
    }

    fun dismissTransfer() {
        _uiState.update { it.copy(transfer = null) }
    }

    fun showMessage(message: String) {
        _messages.tryEmit(message)
    }

    fun loadProperties(path: String) {
        propertiesJob?.cancel()
        _uiState.update {
            it.copy(properties = PropertiesUiState(path = path))
        }
        propertiesJob = viewModelScope.launch {
            repository.properties(path)
                .onSuccess { properties ->
                    _uiState.update {
                        it.copy(
                            properties = PropertiesUiState(
                                path = path,
                                isLoading = false,
                                properties = properties
                            )
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            properties = PropertiesUiState(
                                path = path,
                                isLoading = false,
                                errorMessage = friendlyError(error)
                            )
                        )
                    }
                }
        }
    }

    fun clearProperties() {
        propertiesJob?.cancel()
        propertiesJob = null
        _uiState.update { it.copy(properties = null) }
    }

    private fun loadStorage() {
        _uiState.update { it.copy(storageLoading = true, storageError = null) }
        viewModelScope.launch {
            repository.storageRoots()
                .onSuccess { roots ->
                    _uiState.update {
                        it.copy(storage = roots, storageLoading = false, storageError = null)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            storage = emptyList(),
                            storageLoading = false,
                            storageError = getApplication<Application>().getString(R.string.storage_error)
                        )
                    }
                }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            metadataRepository.preferences.collect { preferences ->
                val previous = _uiState.value.preferences
                val firstEmission = !preferencesInitialized
                preferencesInitialized = true
                _uiState.update {
                    it.copy(
                        preferences = preferences,
                        viewStyle = preferences.viewStyle
                    )
                }
                refreshStoredItems(preferences)

                if (firstEmission) {
                    val rememberedPath = preferences.lastFolder
                    if (preferences.rememberLastFolder &&
                        rememberedPath != null &&
                        repository.isPathInsideStorage(rememberedPath)
                    ) {
                        _uiState.update {
                            it.copy(
                                browserRootPath = repository.storageRootPathFor(rememberedPath)
                                    ?: it.browserRootPath,
                                browser = it.browser.copy(path = rememberedPath)
                            )
                        }
                        if (repository.hasStorageAccess()) openDirectory(rememberedPath)
                    }
                } else if (
                    previous.showHiddenFiles != preferences.showHiddenFiles ||
                    previous.sortOrder != preferences.sortOrder ||
                    previous.foldersFirst != preferences.foldersFirst
                ) {
                    openDirectory(_uiState.value.browser.path)
                }
            }
        }
    }

    private fun refreshStoredItems(preferences: ExplorerPreferences) {
        storedItemsJob?.cancel()
        storedItemsJob = viewModelScope.launch {
            val favorites = withContext(Dispatchers.IO) {
                preferences.favorites.map { StoredFileUi(it, repository.isFileAvailable(it.path)) }
            }
            val recent = withContext(Dispatchers.IO) {
                preferences.recentFiles.map { StoredFileUi(it, repository.isFileAvailable(it.path)) }
            }
            _uiState.update { it.copy(favoriteItems = favorites, recentItems = recent) }
        }
    }

    private fun startTransfer(
        operation: FileOperation,
        paths: List<String>,
        destinationDirectory: String?
    ) {
        val sources = paths.distinct()
        if (sources.isEmpty()) return

        operationJob?.cancel()
        _uiState.update {
            it.copy(
                transfer = TransferUiState(
                    operation = operation,
                    totalItems = sources.size
                )
            )
        }
        operationJob = viewModelScope.launch {
            val result = when (operation) {
                FileOperation.DELETE -> repository.delete(sources, ::updateTransferProgress)
                FileOperation.COPY -> repository.copy(
                    sources,
                    destinationDirectory.orEmpty(),
                    ::updateTransferProgress
                )
                FileOperation.MOVE -> repository.move(
                    sources,
                    destinationDirectory.orEmpty(),
                    ::updateTransferProgress
                )
                FileOperation.COMPRESS, FileOperation.EXTRACT ->
                    Result.failure(IllegalStateException(getApplication<Application>().getString(R.string.operation_failed)))
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(transfer = null) }
                    clearSelection()
                    emitMessage(
                        when (operation) {
                            FileOperation.COPY -> R.string.items_copied
                            FileOperation.MOVE -> R.string.items_moved
                            FileOperation.DELETE -> R.string.items_deleted
                            FileOperation.COMPRESS -> R.string.archive_created
                            FileOperation.EXTRACT -> R.string.archive_extracted
                        }
                    )
                    openDirectory(_uiState.value.browser.path)
                    refreshStoredItems(_uiState.value.preferences)
                    if (operation == FileOperation.DELETE) {
                        if (_uiState.value.largeFiles.files.isNotEmpty()) {
                            scanLargeFiles(_uiState.value.largeFiles.minimumBytes)
                        }
                        if (_uiState.value.duplicates.groups.isNotEmpty()) {
                            scanDuplicates()
                        }
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            transfer = it.transfer?.copy(
                                isRunning = false,
                                errorMessage = friendlyError(error)
                            )
                        )
                    }
                }
        }
    }

    private fun startArchive(
        operation: FileOperation,
        paths: List<String>,
        destinationDirectory: String,
        archiveName: String?,
        extractToNewFolder: Boolean
    ) {
        val sources = paths.distinct()
        if (sources.isEmpty()) return
        operationJob?.cancel()
        _uiState.update {
            it.copy(
                transfer = TransferUiState(
                    operation = operation,
                    totalItems = sources.size
                )
            )
        }
        operationJob = viewModelScope.launch {
            val result = if (operation == FileOperation.COMPRESS) {
                repository.createArchive(
                    paths = sources,
                    destinationDirectory = destinationDirectory,
                    archiveName = archiveName.orEmpty(),
                    onProgress = ::updateTransferProgress
                ).map { }
            } else {
                repository.extractArchive(
                    archivePath = sources.first(),
                    destinationDirectory = destinationDirectory,
                    extractToNewFolder = extractToNewFolder,
                    onProgress = ::updateTransferProgress
                )
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(transfer = null) }
                    clearSelection()
                    emitMessage(
                        if (operation == FileOperation.COMPRESS) {
                            R.string.archive_created
                        } else {
                            R.string.archive_extracted
                        }
                    )
                    repository.invalidateSearchIndex()
                    openDirectory(_uiState.value.browser.path)
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            transfer = it.transfer?.copy(
                                isRunning = false,
                                errorMessage = friendlyError(error)
                            )
                        )
                    }
                }
        }
    }

    private fun updateTransferProgress(progress: TransferProgress) {
        _uiState.update { state ->
            state.copy(
                transfer = state.transfer?.copy(
                    currentName = progress.currentName,
                    completedItems = progress.completedItems
                )
            )
        }
    }

    private fun updateAnalyzerProgress(progress: ScanProgress) {
        _uiState.update {
            it.copy(
                analyzer = it.analyzer.copy(
                    scan = it.analyzer.scan.copy(
                        scannedFiles = progress.scannedFiles,
                        currentPath = progress.currentPath
                    )
                )
            )
        }
    }

    private fun updateLargeFilesProgress(progress: ScanProgress) {
        _uiState.update {
            it.copy(
                largeFiles = it.largeFiles.copy(
                    scan = it.largeFiles.scan.copy(
                        scannedFiles = progress.scannedFiles,
                        currentPath = progress.currentPath
                    )
                )
            )
        }
    }

    private fun updateDuplicateProgress(progress: ScanProgress) {
        _uiState.update {
            it.copy(
                duplicates = it.duplicates.copy(
                    scan = it.duplicates.scan.copy(
                        scannedFiles = progress.scannedFiles,
                        currentPath = progress.currentPath
                    )
                )
            )
        }
    }

    private fun emitMessage(messageRes: Int) {
        _messages.tryEmit(getApplication<Application>().getString(messageRes))
    }

    private fun emitOperationError(error: Throwable) {
        if (error is CancellationException) return
        _messages.tryEmit(friendlyError(error))
    }

    private fun permissionMessage(): String =
        getApplication<Application>().getString(R.string.storage_access_required)

    private fun storageUnavailableMessage(): String =
        getApplication<Application>().getString(R.string.storage_unavailable)

    private fun friendlyError(error: Throwable): String = when (error) {
        is SecurityException -> permissionMessage()
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: getApplication<Application>().getString(R.string.operation_failed)
    }

    private fun networkError(error: Throwable): String {
        val detail = error.message.orEmpty().lowercase()
        return when {
            detail.contains("401") || detail.contains("unauthorized") ->
                getApplication<Application>().getString(R.string.network_auth_failed)
            detail.contains("timeout") || detail.contains("timed out") ->
                getApplication<Application>().getString(R.string.network_timeout)
            else -> getApplication<Application>().getString(R.string.network_connection_failed)
        }
    }

    private fun tabTitle(path: String, rootPath: String): String {
        if (path == rootPath) {
            return _uiState.value.storage.firstOrNull { it.path == rootPath }?.label
                ?: if (rootPath == repository.primaryStoragePath) {
                    getApplication<Application>().getString(R.string.internal_storage)
                } else {
                    File(rootPath).name
                }
        }
        return File(path).name.ifBlank {
            getApplication<Application>().getString(R.string.files)
        }
    }

    private fun ExplorerFile.toReference(lastAccessed: Long? = null): FileReference = FileReference(
        path = path,
        name = name,
        isDirectory = isDirectory,
        kind = kind,
        mimeType = mimeType,
        lastAccessedEpochMillis = lastAccessed
    )

    private companion object {
        const val MAX_TEXT_PREVIEW_BYTES = 2 * 1024 * 1024
    }
}

package com.prakash.pexplorer.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pexplorer.R
import com.prakash.pexplorer.data.filesystem.LocalFileSystemProvider
import com.prakash.pexplorer.data.repository.FileRepository
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.model.ViewMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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

data class ExplorerUiState(
    val storage: List<StorageInfo> = emptyList(),
    val storageLoading: Boolean = true,
    val storageError: String? = null,
    val storageAccessGranted: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val browserRootPath: String,
    val browser: BrowserUiState,
    val transfer: TransferUiState? = null,
    val properties: PropertiesUiState? = null
)

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(LocalFileSystemProvider(application))
    private val _uiState = MutableStateFlow(
        ExplorerUiState(
            browserRootPath = repository.primaryStoragePath,
            browser = BrowserUiState(path = repository.primaryStoragePath)
        )
    )
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var directoryJob: Job? = null
    private var operationJob: Job? = null
    private var propertiesJob: Job? = null

    init {
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
        openDirectory(repository.primaryStoragePath)
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
        _uiState.update {
            it.copy(
                browserRootPath = browserRootPath,
                browser = it.browser.copy(
                    path = path,
                    items = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                    selectedPaths = emptySet()
                )
            )
        }

        if (!repository.hasStorageAccess()) {
            _uiState.update {
                it.copy(browser = it.browser.copy(isLoading = false, errorMessage = permissionMessage()))
            }
            return
        }

        directoryJob = viewModelScope.launch {
            repository.listDirectory(path, showHidden = false)
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

    fun setViewMode(viewMode: ViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
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
                        }
                    )
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
}

package com.prakash.pexplorer.data.repository

import com.prakash.pexplorer.data.filesystem.FileSystemProvider
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.SortOrder
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.model.DuplicateGroup
import com.prakash.pexplorer.domain.model.ScanProgress
import com.prakash.pexplorer.domain.model.StorageAnalysis
import com.prakash.pexplorer.data.filesystem.TextContent
import com.prakash.pexplorer.domain.usecase.FileSorter

class FileRepository(
    private val fileSystemProvider: FileSystemProvider
) {
    suspend fun listDirectory(
        path: String,
        showHidden: Boolean,
        sortOrder: SortOrder,
        foldersFirst: Boolean
    ): Result<List<ExplorerFile>> =
        fileSystemProvider.listDirectory(path, showHidden).map {
            FileSorter.sort(it, sortOrder, foldersFirst)
        }

    suspend fun storageRoots(): Result<List<StorageInfo>> =
        fileSystemProvider.storageRoots()

    suspend fun analyzeStorage(onProgress: (ScanProgress) -> Unit): Result<StorageAnalysis> =
        fileSystemProvider.analyzeStorage(onProgress)

    suspend fun findLargeFiles(
        minimumBytes: Long,
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): Result<List<ExplorerFile>> = fileSystemProvider.findLargeFiles(
        minimumBytes,
        showHidden,
        onProgress
    )

    suspend fun findDuplicates(
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): Result<List<DuplicateGroup>> = fileSystemProvider.findDuplicates(showHidden, onProgress)

    suspend fun createDirectory(parentPath: String, name: String): Result<String> =
        fileSystemProvider.createDirectory(parentPath, name)

    suspend fun rename(path: String, newName: String): Result<String> =
        fileSystemProvider.rename(path, newName)

    suspend fun delete(
        paths: List<String>,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = fileSystemProvider.delete(paths, onProgress)

    suspend fun copy(
        paths: List<String>,
        destinationDirectory: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = fileSystemProvider.copy(paths, destinationDirectory, onProgress)

    suspend fun move(
        paths: List<String>,
        destinationDirectory: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = fileSystemProvider.move(paths, destinationDirectory, onProgress)

    suspend fun properties(path: String): Result<FileProperties> =
        fileSystemProvider.properties(path)

    fun hasStorageAccess(): Boolean = fileSystemProvider.hasStorageAccess()

    fun isPathInsideStorage(path: String): Boolean =
        fileSystemProvider.isPathInsideStorage(path)

    fun storageRootPathFor(path: String): String? =
        fileSystemProvider.storageRootPathFor(path)

    suspend fun search(pathQuery: String, showHidden: Boolean): Result<List<ExplorerFile>> =
        fileSystemProvider.search(pathQuery, showHidden)

    fun invalidateSearchIndex() = fileSystemProvider.invalidateSearchIndex()

    fun isFileAvailable(path: String): Boolean = fileSystemProvider.isFileAvailable(path)

    suspend fun readText(path: String, maxBytes: Int): Result<TextContent> =
        fileSystemProvider.readText(path, maxBytes)

    val primaryStoragePath: String
        get() = fileSystemProvider.primaryStoragePath

    suspend fun createArchive(
        paths: List<String>,
        destinationDirectory: String,
        archiveName: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<String> = fileSystemProvider.createArchive(
        paths,
        destinationDirectory,
        archiveName,
        onProgress
    )

    suspend fun extractArchive(
        archivePath: String,
        destinationDirectory: String,
        extractToNewFolder: Boolean,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = fileSystemProvider.extractArchive(
        archivePath,
        destinationDirectory,
        extractToNewFolder,
        onProgress
    )
}

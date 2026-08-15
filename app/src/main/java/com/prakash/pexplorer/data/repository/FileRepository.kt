package com.prakash.pexplorer.data.repository

import com.prakash.pexplorer.data.filesystem.FileSystemProvider
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.usecase.FileSorter

class FileRepository(
    private val fileSystemProvider: FileSystemProvider
) {
    suspend fun listDirectory(
        path: String,
        showHidden: Boolean
    ): Result<List<ExplorerFile>> =
        fileSystemProvider.listDirectory(path, showHidden).map(FileSorter::foldersFirstByName)

    suspend fun storageRoots(): Result<List<StorageInfo>> =
        fileSystemProvider.storageRoots()

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

    val primaryStoragePath: String
        get() = fileSystemProvider.primaryStoragePath
}

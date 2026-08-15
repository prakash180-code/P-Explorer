package com.prakash.pexplorer.data.filesystem

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.TransferProgress

interface FileSystemProvider {
    val primaryStoragePath: String

    fun hasStorageAccess(): Boolean

    fun isPathInsideStorage(path: String): Boolean

    fun storageRootPathFor(path: String): String?

    suspend fun listDirectory(
        path: String,
        showHidden: Boolean
    ): Result<List<ExplorerFile>>

    suspend fun storageRoots(): Result<List<StorageInfo>>

    suspend fun createDirectory(parentPath: String, name: String): Result<String>

    suspend fun rename(path: String, newName: String): Result<String>

    suspend fun delete(
        paths: List<String>,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit>

    suspend fun copy(
        paths: List<String>,
        destinationDirectory: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit>

    suspend fun move(
        paths: List<String>,
        destinationDirectory: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit>

    suspend fun properties(path: String): Result<FileProperties>
}

package com.prakash.pexplorer.data.filesystem

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.StorageAnalysis
import com.prakash.pexplorer.domain.model.DuplicateGroup
import com.prakash.pexplorer.domain.model.ScanProgress
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

    suspend fun search(query: String, showHidden: Boolean): Result<List<ExplorerFile>>

    suspend fun readText(path: String, maxBytes: Int): Result<TextContent>

    fun invalidateSearchIndex()

    fun isFileAvailable(path: String): Boolean

    suspend fun storageRoots(): Result<List<StorageInfo>>

    suspend fun analyzeStorage(onProgress: (ScanProgress) -> Unit): Result<StorageAnalysis>

    suspend fun findLargeFiles(
        minimumBytes: Long,
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): Result<List<ExplorerFile>>

    suspend fun findDuplicates(
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): Result<List<DuplicateGroup>>

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

    suspend fun createArchive(
        paths: List<String>,
        destinationDirectory: String,
        archiveName: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<String>

    suspend fun extractArchive(
        archivePath: String,
        destinationDirectory: String,
        extractToNewFolder: Boolean,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit>
}

data class TextContent(
    val value: String,
    val isTruncated: Boolean
)

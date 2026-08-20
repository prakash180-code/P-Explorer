package com.prakash.pexplorer.domain.model

enum class StorageCategory {
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    DOWNLOADS,
    APPS,
    ARCHIVES,
    OTHER
}

data class StorageCategoryUsage(
    val category: StorageCategory,
    val bytes: Long,
    val fileCount: Int
)

data class FolderUsage(
    val path: String,
    val name: String,
    val bytes: Long,
    val fileCount: Int
)

data class StorageAnalysis(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categories: List<StorageCategoryUsage>,
    val largestFiles: List<ExplorerFile>,
    val scannedFileCount: Int,
    val categoryFiles: Map<StorageCategory, List<ExplorerFile>> = emptyMap()
)

data class ScanProgress(
    val scannedFiles: Int,
    val currentPath: String
)

data class DuplicateGroup(
    val key: String,
    val files: List<ExplorerFile>
) {
    val totalBytes: Long
        get() = files.sumOf { it.sizeBytes }
}

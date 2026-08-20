package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FolderUsage
import com.prakash.pexplorer.domain.model.SortOrder
import java.util.Locale

object FolderGrouper {

    fun parentOf(path: String): String {
        val index = path.lastIndexOf('/')
        return if (index > 0) path.substring(0, index) else "/"
    }

    fun group(files: List<ExplorerFile>): List<FolderUsage> {
        val byFolder = LinkedHashMap<String, MutableList<ExplorerFile>>()
        files.forEach { file ->
            byFolder.getOrPut(parentOf(file.path)) { mutableListOf() } += file
        }
        return byFolder.map { (folderPath, folderFiles) ->
            FolderUsage(
                path = folderPath,
                name = folderPath.substringAfterLast('/').ifEmpty { folderPath },
                bytes = folderFiles.sumOf { it.sizeBytes },
                fileCount = folderFiles.size
            )
        }.sortedByDescending { it.bytes }
    }

    fun sort(usages: List<FolderUsage>, sortOrder: SortOrder): List<FolderUsage> {
        val comparator: Comparator<FolderUsage> = when (sortOrder) {
            SortOrder.NAME_ASC -> compareBy { it.name.lowercase(Locale.ROOT) }
            SortOrder.NAME_DESC -> compareByDescending { it.name.lowercase(Locale.ROOT) }
            SortOrder.SIZE_LARGEST -> compareByDescending { it.bytes }
            SortOrder.SIZE_SMALLEST -> compareBy { it.bytes }
            SortOrder.DATE_NEWEST, SortOrder.DATE_OLDEST,
            SortOrder.CREATED_NEWEST, SortOrder.CREATED_OLDEST,
            SortOrder.TYPE -> compareByDescending { it.bytes }
        }
        return usages.sortedWith(comparator)
    }
}
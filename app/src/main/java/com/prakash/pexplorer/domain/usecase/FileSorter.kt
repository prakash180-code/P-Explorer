package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.SortOrder
import java.util.Locale

object FileSorter {
    fun foldersFirstByName(files: List<ExplorerFile>): List<ExplorerFile> =
        sort(files, SortOrder.NAME_ASC, foldersFirst = true)

    fun sort(
        files: List<ExplorerFile>,
        sortOrder: SortOrder,
        foldersFirst: Boolean
    ): List<ExplorerFile> {
        val folderComparator = if (foldersFirst) {
            compareBy<ExplorerFile> { if (it.isDirectory) 0 else 1 }
        } else {
            compareBy { 0 }
        }
        val comparator = when (sortOrder) {
            SortOrder.NAME_ASC -> folderComparator.thenBy { it.name.lowercase(Locale.ROOT) }
            SortOrder.NAME_DESC -> folderComparator.thenByDescending { it.name.lowercase(Locale.ROOT) }
            SortOrder.DATE_NEWEST -> folderComparator.thenByDescending { it.modifiedEpochMillis ?: Long.MIN_VALUE }
            SortOrder.DATE_OLDEST -> folderComparator.thenBy { it.modifiedEpochMillis ?: Long.MAX_VALUE }
            SortOrder.SIZE_LARGEST -> folderComparator.thenByDescending { it.sizeBytes }
            SortOrder.SIZE_SMALLEST -> folderComparator.thenBy { it.sizeBytes }
            SortOrder.TYPE -> folderComparator
                .thenBy { it.kind.name }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        }
        return files.sortedWith(comparator)
    }
}

package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class FileSorterTest {
    @Test
    fun foldersComeBeforeFilesAndNamesAreCaseInsensitive() {
        val files = listOf(
            explorerFile("zeta.txt"),
            explorerFile("Beta" , directory = true),
            explorerFile("alpha.txt"),
            explorerFile("alpha-folder", directory = true)
        )

        val sorted = FileSorter.foldersFirstByName(files)

        assertEquals(
            listOf("alpha-folder", "Beta", "alpha.txt", "zeta.txt"),
            sorted.map { it.name }
        )
    }

    @Test
    fun sizeSortingCanMixFoldersAndFiles() {
        val files = listOf(
            explorerFile("small.txt", sizeBytes = 2),
            explorerFile("large.txt", sizeBytes = 20),
            explorerFile("folder", directory = true)
        )

        val sorted = FileSorter.sort(files, SortOrder.SIZE_LARGEST, foldersFirst = false)

        assertEquals(listOf("large.txt", "small.txt", "folder"), sorted.map { it.name })
    }

    @Test
    fun createdSortingOrdersNewestFirst() {
        val files = listOf(
            explorerFile("old.txt", createdEpochMillis = 100L),
            explorerFile("new.txt", createdEpochMillis = 300L),
            explorerFile("mid.txt", createdEpochMillis = 200L)
        )

        val sorted = FileSorter.sort(files, SortOrder.CREATED_NEWEST, foldersFirst = false)

        assertEquals(listOf("new.txt", "mid.txt", "old.txt"), sorted.map { it.name })
    }

    private fun explorerFile(
        name: String,
        directory: Boolean = false,
        sizeBytes: Long = 0L,
        createdEpochMillis: Long? = null
    ) = ExplorerFile(
        path = "/storage/emulated/0/$name",
        name = name,
        isDirectory = directory,
        sizeBytes = sizeBytes,
        modifiedEpochMillis = null,
        createdEpochMillis = createdEpochMillis,
        mimeType = null,
        kind = if (directory) FileKind.FOLDER else FileKind.UNKNOWN
    )
}

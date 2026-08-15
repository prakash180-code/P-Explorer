package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileKind
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

    private fun explorerFile(name: String, directory: Boolean = false) = ExplorerFile(
        path = "/storage/emulated/0/$name",
        name = name,
        isDirectory = directory,
        sizeBytes = 0L,
        modifiedEpochMillis = null,
        mimeType = null,
        kind = if (directory) FileKind.FOLDER else FileKind.UNKNOWN
    )
}

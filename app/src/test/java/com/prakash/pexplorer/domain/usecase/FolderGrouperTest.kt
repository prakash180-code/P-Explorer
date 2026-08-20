package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderGrouperTest {

    @Test
    fun parentOfReturnsParentDirectory() {
        assertEquals(
            "/storage/emulated/0/Download",
            FolderGrouper.parentOf("/storage/emulated/0/Download/movie.mp4")
        )
    }

    @Test
    fun groupSumsBytesAndCountsPerFolder() {
        val files = listOf(
            file("/storage/emulated/0/Download/a.mp4", "a.mp4", 10),
            file("/storage/emulated/0/Download/b.mp4", "b.mp4", 20),
            file("/storage/emulated/0/Music/c.mp3", "c.mp3", 5)
        )

        val groups = FolderGrouper.group(files)

        assertEquals(2, groups.size)
        val download = groups.first { it.path == "/storage/emulated/0/Download" }
        assertEquals(30, download.bytes)
        assertEquals(2, download.fileCount)
        assertEquals("Download", download.name)
        val music = groups.first { it.path == "/storage/emulated/0/Music" }
        assertEquals(5, music.bytes)
        assertEquals(1, music.fileCount)
    }

    @Test
    fun groupSortsByBytesDescendingByDefault() {
        val files = listOf(
            file("/s/a/small.bin", "small.bin", 1),
            file("/s/b/big.bin", "big.bin", 100),
            file("/s/c/mid.bin", "mid.bin", 50)
        )

        val groups = FolderGrouper.group(files)

        assertEquals(listOf("/s/b", "/s/c", "/s/a"), groups.map { it.path })
    }

    @Test
    fun sortSupportsNameAndSizeOrders() {
        val usages = FolderGrouper.group(
            listOf(
                file("/s/zz/file.bin", "file.bin", 50),
                file("/s/aa/file.bin", "file.bin", 100)
            )
        )

        assertEquals(listOf("aa", "zz"), FolderGrouper.sort(usages, SortOrder.NAME_ASC).map { it.name })
        assertEquals(listOf("zz", "aa"), FolderGrouper.sort(usages, SortOrder.NAME_DESC).map { it.name })
        assertEquals(listOf("aa", "zz"), FolderGrouper.sort(usages, SortOrder.SIZE_LARGEST).map { it.name })
        assertEquals(listOf("zz", "aa"), FolderGrouper.sort(usages, SortOrder.SIZE_SMALLEST).map { it.name })
    }

    @Test
    fun groupCapturesLatestModifiedDate() {
        val groups = FolderGrouper.group(
            listOf(
                file("/s/new/a.txt", "a.txt", 10, modified = 2000),
                file("/s/new/b.txt", "b.txt", 20, modified = 1000)
            )
        )

        assertEquals(2000L, groups.single().latestModifiedEpochMillis)
    }

    @Test
    fun sortSupportsDateOrders() {
        val usages = FolderGrouper.group(
            listOf(
                file("/s/older/a.txt", "a.txt", 1, modified = 1000),
                file("/s/newer/b.txt", "b.txt", 1, modified = 3000)
            )
        )

        assertEquals(
            listOf("newer", "older"),
            FolderGrouper.sort(usages, SortOrder.DATE_NEWEST).map { it.name }
        )
        assertEquals(
            listOf("older", "newer"),
            FolderGrouper.sort(usages, SortOrder.DATE_OLDEST).map { it.name }
        )
    }

    private fun file(
        path: String,
        name: String,
        sizeBytes: Long,
        modified: Long? = null
    ) = ExplorerFile(
        path = path,
        name = name,
        isDirectory = false,
        sizeBytes = sizeBytes,
        modifiedEpochMillis = modified,
        mimeType = null,
        kind = FileKind.UNKNOWN
    )
}
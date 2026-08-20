package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileCategory
import com.prakash.pexplorer.domain.model.FileKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileCategoryMatcherTest {
    @Test
    fun videosExcludeOtherFileTypes() {
        val video = file("movie.mp4", FileKind.VIDEO)
        val image = file("photo.jpg", FileKind.IMAGE)
        val document = file("notes.pdf", FileKind.PDF)
        val archive = file("backup.zip", FileKind.ARCHIVE)

        assertTrue(FileCategoryMatcher.matches(video, FileCategory.VIDEOS))
        assertFalse(FileCategoryMatcher.matches(image, FileCategory.VIDEOS))
        assertFalse(FileCategoryMatcher.matches(document, FileCategory.VIDEOS))
        assertFalse(FileCategoryMatcher.matches(archive, FileCategory.VIDEOS))
    }

    @Test
    fun downloadsMatchByPath() {
        assertTrue(
            FileCategoryMatcher.matches(
                file("Download/movie.mp4", FileKind.VIDEO),
                FileCategory.DOWNLOADS
            )
        )
    }

    private fun file(name: String, kind: FileKind) = ExplorerFile(
        path = "/storage/emulated/0/$name",
        name = name.substringAfterLast('/'),
        isDirectory = false,
        sizeBytes = 1L,
        modifiedEpochMillis = null,
        mimeType = null,
        kind = kind
    )
}

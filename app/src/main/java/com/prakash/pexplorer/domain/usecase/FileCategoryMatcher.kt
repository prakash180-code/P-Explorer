package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileCategory
import com.prakash.pexplorer.domain.model.FileKind

object FileCategoryMatcher {
    fun matches(file: ExplorerFile, category: FileCategory): Boolean = when (category) {
        FileCategory.IMAGES -> file.kind == FileKind.IMAGE
        FileCategory.VIDEOS -> file.kind == FileKind.VIDEO
        FileCategory.AUDIO -> file.kind == FileKind.AUDIO
        FileCategory.DOCUMENTS -> file.kind in setOf(
            FileKind.DOCUMENT,
            FileKind.SPREADSHEET,
            FileKind.PRESENTATION,
            FileKind.PDF,
            FileKind.TEXT
        )
        FileCategory.DOWNLOADS -> file.path.split('/', '\\')
            .any { it.equals("Download", ignoreCase = true) }
        FileCategory.APKS -> file.kind == FileKind.APK
        FileCategory.ARCHIVES -> file.kind == FileKind.ARCHIVE
    }
}

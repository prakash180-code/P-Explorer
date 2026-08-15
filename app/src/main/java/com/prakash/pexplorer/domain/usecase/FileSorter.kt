package com.prakash.pexplorer.domain.usecase

import com.prakash.pexplorer.domain.model.ExplorerFile
import java.util.Locale

object FileSorter {
    fun foldersFirstByName(files: List<ExplorerFile>): List<ExplorerFile> =
        files.sortedWith(
            compareBy<ExplorerFile> { if (it.isDirectory) 0 else 1 }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
}

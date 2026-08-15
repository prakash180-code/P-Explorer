package com.prakash.pexplorer.domain.model

data class ExplorerFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedEpochMillis: Long?,
    val mimeType: String?,
    val kind: FileKind
) {
    val isHidden: Boolean
        get() = name.startsWith('.')
}

package com.prakash.pexplorer.domain.model

data class FileReference(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val kind: FileKind,
    val mimeType: String?,
    val lastAccessedEpochMillis: Long? = null
)

fun FileReference.toExplorerFile(): ExplorerFile = ExplorerFile(
    path = path,
    name = name,
    isDirectory = isDirectory,
    sizeBytes = 0L,
    modifiedEpochMillis = null,
    mimeType = mimeType,
    kind = kind
)

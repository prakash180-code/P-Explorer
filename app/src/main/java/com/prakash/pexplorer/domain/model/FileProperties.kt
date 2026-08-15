package com.prakash.pexplorer.domain.model

data class FileProperties(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val kind: FileKind,
    val sizeBytes: Long,
    val itemCount: Int?,
    val createdEpochMillis: Long?,
    val modifiedEpochMillis: Long?,
    val accessedEpochMillis: Long?,
    val mimeType: String?
)

package com.prakash.pexplorer.domain.model

data class StorageInfo(
    val label: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean
) {
    val usedBytes: Long
        get() = (totalBytes - freeBytes).coerceAtLeast(0L)

    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toDouble() / totalBytes).toFloat() else 0f
}

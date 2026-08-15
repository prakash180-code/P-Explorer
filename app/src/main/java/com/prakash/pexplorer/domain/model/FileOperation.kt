package com.prakash.pexplorer.domain.model

enum class FileOperation {
    COPY,
    MOVE,
    DELETE
}

data class TransferProgress(
    val operation: FileOperation,
    val currentName: String,
    val completedItems: Int,
    val totalItems: Int
) {
    val fraction: Float
        get() = if (totalItems > 0) {
            (completedItems.toFloat() / totalItems).coerceIn(0f, 1f)
        } else {
            0f
        }
}

package com.prakash.pexplorer.domain.model

enum class TransferTaskStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TransferTask(
    val id: String,
    val operation: FileOperation,
    val sourcePaths: List<String>,
    val destinationPath: String?,
    val status: TransferTaskStatus = TransferTaskStatus.QUEUED,
    val completedItems: Int = 0,
    val totalItems: Int = sourcePaths.size,
    val currentName: String? = null,
    val errorMessage: String? = null
)

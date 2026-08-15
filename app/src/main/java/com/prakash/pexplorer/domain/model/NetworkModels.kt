package com.prakash.pexplorer.domain.model

data class NetworkDevice(
    val name: String,
    val host: String,
    val port: Int = 8080,
    val serviceType: String = "_phub._tcp."
)

data class NetworkFileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedEpochMillis: Long?
)

data class NetworkTransferProgress(
    val fileName: String,
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val fraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toDouble() / totalBytes).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

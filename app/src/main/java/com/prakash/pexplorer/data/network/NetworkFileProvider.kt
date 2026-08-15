package com.prakash.pexplorer.data.network

import com.prakash.pexplorer.domain.model.NetworkDevice
import com.prakash.pexplorer.domain.model.NetworkFileEntry
import com.prakash.pexplorer.domain.model.NetworkTransferProgress
import java.io.File

interface NetworkFileProvider {
    suspend fun discover(): Result<List<NetworkDevice>>

    suspend fun requestChallenge(device: NetworkDevice): Result<List<String>>

    suspend fun connect(device: NetworkDevice, authCode: String): Result<List<NetworkFileEntry>>

    suspend fun list(path: String): Result<List<NetworkFileEntry>>

    suspend fun download(
        entry: NetworkFileEntry,
        onProgress: (NetworkTransferProgress) -> Unit
    ): Result<File>

    suspend fun upload(
        localFile: File,
        remotePath: String,
        onProgress: (NetworkTransferProgress) -> Unit
    ): Result<Unit>

    fun disconnect()
}

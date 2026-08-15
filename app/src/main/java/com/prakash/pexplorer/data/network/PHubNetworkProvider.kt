package com.prakash.pexplorer.data.network

import android.content.Context
import android.os.Environment
import com.prakash.pexplorer.domain.model.NetworkDevice
import com.prakash.pexplorer.domain.model.NetworkFileEntry
import com.prakash.pexplorer.domain.model.NetworkTransferProgress
import com.prakash.pexplorer.domain.usecase.UniqueFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.coroutineContext

class PHubNetworkProvider(
    private val context: Context,
    private val discoveryService: PHubDiscoveryService = PHubDiscoveryService(context)
) : NetworkFileProvider {
    private var connectedDevice: NetworkDevice? = null
    private var authCode: String? = null

    override suspend fun discover(): Result<List<NetworkDevice>> = discoveryService.discover().map { services ->
        services.map { service ->
            NetworkDevice(name = service.name, host = service.host, port = service.port)
        }
    }

    override suspend fun requestChallenge(device: NetworkDevice): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = request(device, "/challenge", emptyMap())
                PHubJsonParser.parseChallenge(response)
            }
        }

    override suspend fun connect(
        device: NetworkDevice,
        authCode: String
    ): Result<List<NetworkFileEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = request(device, "/roots", mapOf("auth" to authCode))
            val roots = PHubJsonParser.parseEntries(response)
            connectedDevice = device
            this@PHubNetworkProvider.authCode = authCode
            roots
        }
    }

    override suspend fun list(path: String): Result<List<NetworkFileEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val device = connectedDevice ?: error("Not connected to P-Hub")
            val auth = authCode ?: error("Not authenticated")
            PHubJsonParser.parseEntries(request(device, "/ls", mapOf("auth" to auth, "path" to path)))
        }
    }

    override suspend fun download(
        entry: NetworkFileEntry,
        onProgress: (NetworkTransferProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val device = connectedDevice ?: error("Not connected to P-Hub")
            val auth = authCode ?: error("Not authenticated")
            if (entry.isDirectory) error("A folder cannot be downloaded as a file")
            val destinationDirectory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "P-Hub"
            )
            if (!destinationDirectory.exists() && !destinationDirectory.mkdirs()) {
                error("Could not create the P-Hub download folder")
            }
            val destinationName = UniqueFileName.nextAvailableName(entry.name) {
                File(destinationDirectory, it).exists()
            }
            val destination = File(destinationDirectory, destinationName)
            val connection = openConnection(
                device,
                "/download",
                mapOf("auth" to auth, "path" to entry.path)
            )
            try {
                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L
                connection.inputStream.use { input ->
                    BufferedInputStream(input).use { bufferedInput ->
                        FileOutputStream(destination).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                coroutineContext.ensureActive()
                                val count = bufferedInput.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                downloadedBytes += count
                                onProgress(
                                    NetworkTransferProgress(
                                        fileName = entry.name,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                )
                            }
                        }
                    }
                }
                destination
            } catch (error: Throwable) {
                destination.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun upload(
        localFile: File,
        remotePath: String,
        onProgress: (NetworkTransferProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val device = connectedDevice ?: error("Not connected to P-Hub")
            val auth = authCode ?: error("Not authenticated")
            if (!localFile.exists() || !localFile.isFile) error("Selected file is unavailable")
            val boundary = "----PExplorer${System.currentTimeMillis()}"
            val header = "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"${localFile.name}\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n"
            val footer = "\r\n--$boundary--\r\n"
            val connection = openUploadConnection(
                device = device,
                query = mapOf("auth" to auth, "path" to remotePath, "name" to "P-Explorer"),
                boundary = boundary,
                contentLength = header.toByteArray(StandardCharsets.UTF_8).size + localFile.length() +
                    footer.toByteArray(StandardCharsets.UTF_8).size
            )
            try {
                var uploadedBytes = 0L
                connection.outputStream.use { output ->
                    output.write(header.toByteArray(StandardCharsets.UTF_8))
                    FileInputStream(localFile).use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            uploadedBytes += count
                            onProgress(
                                NetworkTransferProgress(localFile.name, uploadedBytes, localFile.length())
                            )
                        }
                    }
                    output.write(footer.toByteArray(StandardCharsets.UTF_8))
                }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) error("P-Hub upload failed with HTTP $responseCode")
            } finally {
                connection.disconnect()
            }
        }
    }

    override fun disconnect() {
        connectedDevice = null
        authCode = null
    }

    private fun request(
        device: NetworkDevice,
        endpoint: String,
        query: Map<String, String>
    ): String {
        val connection = openConnection(device, endpoint, query)
        return try {
            connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        device: NetworkDevice,
        endpoint: String,
        query: Map<String, String>
    ): HttpURLConnection {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val url = URL("http://${device.host}:${device.port}$endpoint?$queryString")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 30_000
            doInput = true
            connect()
            if (responseCode !in 200..299) {
                disconnect()
                error("P-Hub request failed with HTTP $responseCode")
            }
        }
    }

    private fun openUploadConnection(
        device: NetworkDevice,
        query: Map<String, String>,
        boundary: String,
        contentLength: Long
    ): HttpURLConnection {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val url = URL("http://${device.host}:${device.port}/upload?$queryString")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 30_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setFixedLengthStreamingMode(contentLength)
            connect()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

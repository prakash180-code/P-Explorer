package com.prakash.pexplorer.data.filesystem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.usecase.FileNameError
import com.prakash.pexplorer.domain.usecase.FileNameValidator
import com.prakash.pexplorer.domain.usecase.UniqueFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.Locale
import android.os.StatFs
import java.util.ArrayDeque
import kotlin.coroutines.coroutineContext

class LocalFileSystemProvider(
    private val context: Context
) : FileSystemProvider {

    override val primaryStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    override fun hasStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun isPathInsideStorage(path: String): Boolean {
        val candidate = runCatching { File(path).canonicalFile }.getOrNull() ?: return false
        return storageDirectories().any { storage ->
            val root = runCatching { storage.directory.canonicalFile }.getOrNull() ?: return@any false
            candidate == root || candidate.path.startsWith(root.path + File.separator)
        }
    }

    override fun storageRootPathFor(path: String): String? {
        val candidate = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
        return storageDirectories().firstOrNull { storage ->
            val root = runCatching { storage.directory.canonicalFile }.getOrNull() ?: return@firstOrNull false
            candidate == root || candidate.path.startsWith(root.path + File.separator)
        }?.directory?.absolutePath
    }

    override suspend fun listDirectory(
        path: String,
        showHidden: Boolean
    ): Result<List<ExplorerFile>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasStorageAccess()) {
                throw SecurityException(context.getString(R.string.storage_access_required))
            }
            if (!isPathInsideStorage(path)) {
                throw SecurityException(context.getString(R.string.storage_unavailable))
            }

            val directory = File(path)
            if (!directory.exists() || !directory.isDirectory) {
                throw IOException(context.getString(R.string.could_not_open_folder))
            }

            directory.listFiles()
                ?.asSequence()
                ?.filter { showHidden || !it.name.startsWith('.') }
                ?.map(::toExplorerFile)
                ?.toList()
                ?: throw IOException(context.getString(R.string.could_not_open_folder))
        }
    }

    override suspend fun storageRoots(): Result<List<StorageInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            storageDirectories().mapNotNull { storage ->
                val stat = runCatching { StatFs(storage.directory.path) }.getOrNull() ?: return@mapNotNull null
                val totalBytes = stat.totalBytes
                if (totalBytes <= 0L) return@mapNotNull null
                StorageInfo(
                    label = storage.label,
                    path = storage.directory.absolutePath,
                    totalBytes = totalBytes,
                    freeBytes = stat.availableBytes.coerceAtMost(totalBytes),
                    isRemovable = storage.isRemovable
                )
            }
        }
    }

    override suspend fun createDirectory(parentPath: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                validateName(name)
                val parent = resolveDirectory(parentPath)
                val directory = File(parent, name).canonicalFile
                ensureInsideStorage(directory)
                if (directory.exists()) {
                    throw IOException(context.getString(R.string.duplicate_name))
                }
                if (!directory.mkdir()) {
                    throw IOException(context.getString(R.string.operation_failed))
                }
                directory.absolutePath
            }
        }

    override suspend fun rename(path: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                validateName(newName)
                val source = resolveExisting(path)
                if (isStorageRoot(source)) {
                    throw IOException(context.getString(R.string.root_operation_not_allowed))
                }
                if (source.name == newName) return@runCatching source.absolutePath
                val parent = source.parentFile?.canonicalFile
                    ?: throw IOException(context.getString(R.string.operation_failed))
                val destination = File(parent, newName).canonicalFile
                ensureInsideStorage(destination)
                if (destination.exists()) {
                    throw IOException(context.getString(R.string.duplicate_name))
                }
                if (!source.renameTo(destination)) {
                    throw IOException(context.getString(R.string.operation_failed))
                }
                destination.absolutePath
            }
        }

    override suspend fun delete(
        paths: List<String>,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            val sources = paths.distinct().map(::resolveExisting)
            val total = sources.size
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                if (isStorageRoot(source)) {
                    throw IOException(context.getString(R.string.root_operation_not_allowed))
                }
                deleteEntry(source)
                onProgress(
                    TransferProgress(
                        operation = FileOperation.DELETE,
                        currentName = source.name,
                        completedItems = index + 1,
                        totalItems = total
                    )
                )
            }
        }
    }

    override suspend fun copy(
        paths: List<String>,
        destinationDirectory: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            val destination = resolveDirectory(destinationDirectory)
            val sources = paths.distinct().map(::resolveExisting)
            val total = sources.size
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                if (source.isDirectory && isSameOrDescendant(destination, source)) {
                    throw IOException(context.getString(R.string.cannot_move_into_itself))
                }
                val target = uniqueDestination(destination, source.name)
                copyEntry(source, target)
                onProgress(
                    TransferProgress(
                        operation = FileOperation.COPY,
                        currentName = source.name,
                        completedItems = index + 1,
                        totalItems = total
                    )
                )
            }
        }
    }

    override suspend fun move(
        paths: List<String>,
        destinationDirectory: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            val destination = resolveDirectory(destinationDirectory)
            val sources = paths.distinct().map(::resolveExisting)
            val total = sources.size
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                if (source.isDirectory && isSameOrDescendant(destination, source)) {
                    throw IOException(context.getString(R.string.cannot_move_into_itself))
                }

                val sourceParent = source.parentFile?.canonicalFile
                if (sourceParent == destination) {
                    onProgress(
                        TransferProgress(
                            operation = FileOperation.MOVE,
                            currentName = source.name,
                            completedItems = index + 1,
                            totalItems = total
                        )
                    )
                    return@forEachIndexed
                }

                val target = uniqueDestination(destination, source.name)
                if (!source.renameTo(target)) {
                    copyEntry(source, target)
                    deleteEntry(source)
                }
                onProgress(
                    TransferProgress(
                        operation = FileOperation.MOVE,
                        currentName = source.name,
                        completedItems = index + 1,
                        totalItems = total
                    )
                )
            }
        }
    }

    override suspend fun properties(path: String): Result<FileProperties> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                val file = resolveExisting(path)
                val explorerFile = toExplorerFile(file)
                val attributes = runCatching {
                    Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                }.getOrNull()
                val folderStats = if (file.isDirectory) folderStats(file) else null
                FileProperties(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    kind = explorerFile.kind,
                    sizeBytes = folderStats?.sizeBytes ?: file.length(),
                    itemCount = folderStats?.itemCount,
                    createdEpochMillis = attributes?.creationTime()?.toMillis(),
                    modifiedEpochMillis = attributes?.lastModifiedTime()?.toMillis()
                        ?: file.lastModified().takeIf { it > 0L },
                    accessedEpochMillis = attributes?.lastAccessTime()?.toMillis(),
                    mimeType = explorerFile.mimeType
                )
            }
        }

    private fun requireStorageAccess() {
        if (!hasStorageAccess()) {
            throw SecurityException(context.getString(R.string.storage_access_required))
        }
    }

    private fun resolveExisting(path: String): File {
        val file = File(path).canonicalFile
        ensureInsideStorage(file)
        if (!file.exists()) {
            throw IOException(context.getString(R.string.file_not_found))
        }
        return file
    }

    private fun resolveDirectory(path: String): File {
        val directory = resolveExisting(path)
        if (!directory.isDirectory) {
            throw IOException(context.getString(R.string.destination_not_folder))
        }
        return directory
    }

    private fun ensureInsideStorage(file: File) {
        if (!isPathInsideStorage(file.absolutePath)) {
            throw SecurityException(context.getString(R.string.storage_unavailable))
        }
    }

    private fun isStorageRoot(file: File): Boolean =
        storageRootPathFor(file.absolutePath)?.let { root ->
            runCatching { File(root).canonicalFile == file.canonicalFile }.getOrDefault(false)
        } == true

    private fun isSameOrDescendant(candidate: File, parent: File): Boolean =
        candidate == parent || candidate.path.startsWith(parent.path + File.separator)

    private fun validateName(name: String) {
        when (FileNameValidator.validate(name)) {
            null -> Unit
            FileNameError.EMPTY -> throw IllegalArgumentException(context.getString(R.string.invalid_name_empty))
            FileNameError.DOT_NAME -> throw IllegalArgumentException(context.getString(R.string.invalid_name_dot))
            FileNameError.INVALID_CHARACTER ->
                throw IllegalArgumentException(context.getString(R.string.invalid_name_characters))
        }
    }

    private fun uniqueDestination(directory: File, name: String): File =
        File(
            directory,
            UniqueFileName.nextAvailableName(name) { candidate ->
                File(directory, candidate).exists()
            }
        )

    private suspend fun copyEntry(source: File, target: File) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            if (!target.mkdir()) {
                throw IOException(context.getString(R.string.operation_failed))
            }
            source.listFiles()?.forEach { child ->
                copyEntry(child, File(target, child.name))
            } ?: throw IOException(context.getString(R.string.operation_failed))
        } else if (!source.copyTo(target, overwrite = false).exists()) {
            throw IOException(context.getString(R.string.operation_failed))
        }
    }

    private suspend fun deleteEntry(file: File) {
        coroutineContext.ensureActive()
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> deleteEntry(child) }
                ?: throw IOException(context.getString(R.string.operation_failed))
        }
        if (!file.delete()) {
            throw IOException(context.getString(R.string.operation_failed))
        }
    }

    private suspend fun folderStats(root: File): FolderStats {
        val pending = ArrayDeque<File>()
        pending.add(root)
        var sizeBytes = 0L
        var itemCount = 0
        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = pending.removeFirst()
            current.listFiles()?.forEach { child ->
                itemCount++
                if (child.isDirectory) {
                    pending.addLast(child)
                } else {
                    sizeBytes += child.length()
                }
            }
        }
        return FolderStats(sizeBytes = sizeBytes, itemCount = itemCount)
    }

    private data class FolderStats(
        val sizeBytes: Long,
        val itemCount: Int
    )

    private fun storageDirectories(): List<StorageDirectory> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageManager = context.getSystemService(StorageManager::class.java)
            val volumes = storageManager?.storageVolumes.orEmpty()
            val directories = volumes.mapNotNull { volume ->
                val directory = volume.directory ?: return@mapNotNull null
                val label = when {
                    volume.isPrimary -> context.getString(R.string.internal_storage)
                    volume.getDescription(context).isNullOrBlank() && volume.isRemovable ->
                        context.getString(R.string.sd_card)
                    else -> volume.getDescription(context)
                }
                StorageDirectory(
                    directory = directory,
                    label = label,
                    isRemovable = volume.isRemovable && !volume.isPrimary
                )
            }
            if (directories.isNotEmpty()) {
                return directories.distinctBy { it.directory.absolutePath }
            }
        }

        return listOf(
            StorageDirectory(
                directory = Environment.getExternalStorageDirectory(),
                label = context.getString(R.string.internal_storage),
                isRemovable = false
            )
        )
    }

    private fun toExplorerFile(file: File): ExplorerFile {
        val extension = file.extension.lowercase(Locale.ROOT)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return ExplorerFile(
            path = file.absolutePath,
            name = file.name,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isFile) file.length() else 0L,
            modifiedEpochMillis = file.lastModified().takeIf { it > 0L },
            mimeType = mimeType,
            kind = fileKind(file, extension, mimeType)
        )
    }

    private fun fileKind(file: File, extension: String, mimeType: String?): FileKind {
        if (file.isDirectory) return FileKind.FOLDER
        return when {
            mimeType?.startsWith("image/") == true || extension in IMAGE_EXTENSIONS -> FileKind.IMAGE
            mimeType?.startsWith("video/") == true || extension in VIDEO_EXTENSIONS -> FileKind.VIDEO
            mimeType?.startsWith("audio/") == true || extension in AUDIO_EXTENSIONS -> FileKind.AUDIO
            extension == "pdf" -> FileKind.PDF
            extension in DOCUMENT_EXTENSIONS -> FileKind.DOCUMENT
            extension in SPREADSHEET_EXTENSIONS -> FileKind.SPREADSHEET
            extension in PRESENTATION_EXTENSIONS -> FileKind.PRESENTATION
            extension in TEXT_EXTENSIONS || mimeType?.startsWith("text/") == true -> FileKind.TEXT
            extension in ARCHIVE_EXTENSIONS -> FileKind.ARCHIVE
            extension == "apk" -> FileKind.APK
            else -> FileKind.UNKNOWN
        }
    }

    private data class StorageDirectory(
        val directory: File,
        val label: String,
        val isRemovable: Boolean
    )

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg")
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
        val AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "flac", "ogg", "aac", "opus", "wma")
        val DOCUMENT_EXTENSIONS = setOf("doc", "docx", "odt", "rtf")
        val SPREADSHEET_EXTENSIONS = setOf("xls", "xlsx", "ods", "csv")
        val PRESENTATION_EXTENSIONS = setOf("ppt", "pptx", "odp")
        val TEXT_EXTENSIONS = setOf("txt", "log", "md", "json", "xml", "yaml", "yml", "ini")
        val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2")
    }
}

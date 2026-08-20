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
import com.prakash.pexplorer.domain.model.FileCategory
import com.prakash.pexplorer.domain.model.DuplicateGroup
import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.domain.model.ScanProgress
import com.prakash.pexplorer.domain.model.StorageInfo
import com.prakash.pexplorer.domain.model.StorageAnalysis
import com.prakash.pexplorer.domain.model.StorageCategory
import com.prakash.pexplorer.domain.model.StorageCategoryUsage
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.usecase.FileNameError
import com.prakash.pexplorer.domain.usecase.FileCategoryMatcher
import com.prakash.pexplorer.domain.usecase.FileNameValidator
import com.prakash.pexplorer.domain.usecase.UniqueFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.StatFs
import java.util.ArrayDeque
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

class LocalFileSystemProvider(
    private val context: Context
) : FileSystemProvider {

    @Volatile
    private var searchIndex: List<ExplorerFile>? = null

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

    override suspend fun search(
        query: String,
        showHidden: Boolean,
        category: FileCategory?
    ): Result<List<ExplorerFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                val normalizedQuery = query.trim().lowercase(Locale.ROOT)
                if (normalizedQuery.isBlank() && category == null) return@runCatching emptyList()
                val index = searchIndex ?: buildSearchIndex().also { searchIndex = it }
                index.asSequence()
                    .filter { showHidden || !isInHiddenPath(it.path) }
                    .filter {
                        category == null || (
                            FileCategoryMatcher.matches(it, category) &&
                                !isSuppressedByNoMedia(it, category)
                            )
                    }
                    .filter { file ->
                        val name = file.name.lowercase(Locale.ROOT)
                        val path = file.path.lowercase(Locale.ROOT)
                        val extension = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                        val extensionQuery = normalizedQuery.removePrefix(".")
                        val year = file.modifiedEpochMillis?.let {
                            SimpleDateFormat("yyyy", Locale.ROOT).format(Date(it))
                        }
                        normalizedQuery.isBlank() ||
                            name.contains(normalizedQuery) ||
                            path.contains(normalizedQuery) ||
                            (extensionQuery.isNotBlank() && extension.contains(extensionQuery)) ||
                            year?.contains(normalizedQuery) == true
                    }
                    .sortedWith(compareBy<ExplorerFile> { if (it.isDirectory) 0 else 1 }
                        .thenBy { it.name.lowercase(Locale.ROOT) })
                    .toList()
            }
        }

    override suspend fun readText(path: String, maxBytes: Int): Result<TextContent> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                val file = resolveExisting(path)
                if (!file.isFile) throw IOException(context.getString(R.string.preview_not_available))
                val limit = maxBytes.coerceAtLeast(1)
                val bytes = ByteArray(limit + 1)
                var offset = 0
                FileInputStream(file).use { input ->
                    while (offset < bytes.size) {
                        val count = input.read(bytes, offset, bytes.size - offset)
                        if (count < 0) break
                        offset += count
                    }
                }
                TextContent(
                    value = String(bytes, 0, minOf(offset, limit), StandardCharsets.UTF_8),
                    isTruncated = offset > limit
                )
            }
        }

    override fun invalidateSearchIndex() {
        searchIndex = null
    }

    override fun isFileAvailable(path: String): Boolean =
        hasStorageAccess() && runCatching {
            val file = File(path).canonicalFile
            file.exists() && isPathInsideStorage(file.path)
        }.getOrDefault(false)

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

    override suspend fun analyzeStorage(onProgress: (ScanProgress) -> Unit): Result<StorageAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                val roots = storageRoots().getOrThrow()
                val files = scanAllFiles(showHidden = false, onProgress)
                val usage = StorageCategory.entries.associateWith { StorageCategoryUsage(it, 0L, 0) }
                    .toMutableMap()
                files.forEach { file ->
                    val category = categoryFor(file)
                    val current = usage.getValue(category)
                    usage[category] = current.copy(
                        bytes = current.bytes + file.sizeBytes,
                        fileCount = current.fileCount + 1
                    )
                }
                val totalBytes = roots.sumOf { it.totalBytes }
                val freeBytes = roots.sumOf { it.freeBytes }
                StorageAnalysis(
                    totalBytes = totalBytes,
                    usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L),
                    freeBytes = freeBytes,
                    categories = StorageCategory.entries.map { usage.getValue(it) },
                    largestFiles = files.sortedByDescending { it.sizeBytes }.take(100),
                    scannedFileCount = files.size
                )
            }
        }

    override suspend fun findLargeFiles(
        minimumBytes: Long,
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): Result<List<ExplorerFile>> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            scanAllFiles(showHidden, onProgress)
                .asSequence()
                .filter { it.sizeBytes >= minimumBytes }
                .sortedByDescending { it.sizeBytes }
                .take(500)
                .toList()
        }
    }

    override suspend fun findDuplicates(
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): Result<List<DuplicateGroup>> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            scanAllFiles(showHidden, onProgress)
                .groupBy { file -> "${file.name.lowercase(Locale.ROOT)}:${file.sizeBytes}" }
                .filterValues { files -> files.size > 1 }
                .map { (key, files) -> DuplicateGroup(key = key, files = files) }
                .sortedByDescending { it.totalBytes }
        }
    }

    override suspend fun createDirectory(parentPath: String, name: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireStorageAccess()
                invalidateSearchIndex()
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
                invalidateSearchIndex()
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
            invalidateSearchIndex()
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
            invalidateSearchIndex()
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
            invalidateSearchIndex()
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
                    mimeType = explorerFile.mimeType,
                    isReadable = file.canRead(),
                    isWritable = file.canWrite(),
                    isExecutable = file.canExecute()
                )
            }
        }

    override suspend fun createArchive(
        paths: List<String>,
        destinationDirectory: String,
        archiveName: String,
        onProgress: (TransferProgress) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            invalidateSearchIndex()
            val destination = resolveDirectory(destinationDirectory)
            val sources = paths.distinct().map(::resolveExisting)
            if (sources.isEmpty()) throw IOException(context.getString(R.string.operation_failed))
            val requestedName = archiveName.trim().let {
                if (it.lowercase(Locale.ROOT).endsWith(".zip")) it else "$it.zip"
            }
            validateName(requestedName)
            val archive = uniqueDestination(destination, requestedName)
            ZipOutputStream(BufferedOutputStream(FileOutputStream(archive))).use { output ->
                sources.forEachIndexed { index, source ->
                    addToArchive(source, source.name, output)
                    onProgress(
                        TransferProgress(
                            operation = FileOperation.COMPRESS,
                            currentName = source.name,
                            completedItems = index + 1,
                            totalItems = sources.size
                        )
                    )
                }
            }
            archive.absolutePath
        }
    }

    override suspend fun extractArchive(
        archivePath: String,
        destinationDirectory: String,
        extractToNewFolder: Boolean,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireStorageAccess()
            invalidateSearchIndex()
            val archive = resolveExisting(archivePath)
            val destination = resolveDirectory(destinationDirectory)
            val extractionRoot = if (extractToNewFolder) {
                val folderName = archive.name.substringBeforeLast('.', archive.name)
                val folder = uniqueDestination(destination, folderName)
                if (!folder.mkdir()) throw IOException(context.getString(R.string.operation_failed))
                folder
            } else {
                destination
            }
            ZipFile(archive).use { zipFile ->
                val entries = mutableListOf<ZipEntry>()
                val enumeration = zipFile.entries()
                while (enumeration.hasMoreElements()) entries += enumeration.nextElement()
                entries.forEachIndexed { index, entry ->
                    coroutineContext.ensureActive()
                    val entryName = entry.name.replace('\\', '/')
                    val target = File(extractionRoot, entryName).canonicalFile
                    if (!isSameOrDescendant(target, extractionRoot)) {
                        throw IOException(context.getString(R.string.archive_path_not_allowed))
                    }
                    if (entry.isDirectory) {
                        if (!target.exists() && !target.mkdirs()) {
                            throw IOException(context.getString(R.string.operation_failed))
                        }
                    } else {
                        val parent = target.parentFile?.canonicalFile
                            ?: throw IOException(context.getString(R.string.operation_failed))
                        if (!parent.exists() && !parent.mkdirs()) {
                            throw IOException(context.getString(R.string.operation_failed))
                        }
                        val outputFile = if (target.exists()) {
                            uniqueDestination(parent, target.name)
                        } else {
                            target
                        }
                        zipFile.getInputStream(entry).use { input ->
                            BufferedInputStream(input).use { bufferedInput ->
                                FileOutputStream(outputFile).use { output ->
                                    BufferedOutputStream(output).use { bufferedOutput ->
                                        bufferedInput.copyTo(bufferedOutput)
                                    }
                                }
                            }
                        }
                    }
                    onProgress(
                        TransferProgress(
                            operation = FileOperation.EXTRACT,
                            currentName = entry.name,
                            completedItems = index + 1,
                            totalItems = entries.size
                        )
                    )
                }
            }
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

    private suspend fun addToArchive(source: File, entryName: String, output: ZipOutputStream) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            val directoryEntry = ZipEntry(entryName.trimEnd('/') + "/")
            output.putNextEntry(directoryEntry)
            output.closeEntry()
            source.listFiles()?.forEach { child ->
                addToArchive(child, "$entryName/${child.name}", output)
            } ?: throw IOException(context.getString(R.string.operation_failed))
        } else {
            output.putNextEntry(ZipEntry(entryName))
            BufferedInputStream(FileInputStream(source)).use { input ->
                input.copyTo(output)
            }
            output.closeEntry()
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

    private suspend fun buildSearchIndex(): List<ExplorerFile> {
        val pending = ArrayDeque<File>()
        val visited = mutableSetOf<String>()
        storageDirectories().forEach { pending.add(it.directory) }
        val indexed = mutableListOf<ExplorerFile>()
        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = pending.removeFirst()
            val canonicalPath = runCatching { directory.canonicalPath }.getOrNull() ?: continue
            if (!visited.add(canonicalPath)) continue
            directory.listFiles()?.forEach { child ->
                coroutineContext.ensureActive()
                val explorerFile = toExplorerFile(child)
                indexed += explorerFile
                if (child.isDirectory) pending.addLast(child)
            }
        }
        return indexed
    }

    private suspend fun scanAllFiles(
        showHidden: Boolean,
        onProgress: (ScanProgress) -> Unit
    ): List<ExplorerFile> {
        val pending = ArrayDeque<File>()
        val visited = mutableSetOf<String>()
        storageDirectories().forEach { pending.add(it.directory) }
        val files = mutableListOf<ExplorerFile>()
        var scannedFiles = 0
        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = pending.removeFirst()
            val canonicalPath = runCatching { directory.canonicalPath }.getOrNull() ?: continue
            if (!visited.add(canonicalPath)) continue
            directory.listFiles()?.forEach { child ->
                coroutineContext.ensureActive()
                if (child.isDirectory) {
                    if (showHidden || !child.name.startsWith('.')) pending.addLast(child)
                } else if (showHidden || !isInHiddenPath(child.path)) {
                    val file = toExplorerFile(child)
                    files += file
                    scannedFiles++
                    if (scannedFiles == 1 || scannedFiles % 100 == 0) {
                        onProgress(ScanProgress(scannedFiles, file.path))
                    }
                }
            }
        }
        return files
    }

    private fun categoryFor(file: ExplorerFile): StorageCategory {
        if (file.path.split('/', '\\').any { it.equals("download", ignoreCase = true) }) {
            return StorageCategory.DOWNLOADS
        }
        return when (file.kind) {
            FileKind.IMAGE -> StorageCategory.IMAGES
            FileKind.VIDEO -> StorageCategory.VIDEOS
            FileKind.AUDIO -> StorageCategory.AUDIO
            FileKind.DOCUMENT,
            FileKind.SPREADSHEET,
            FileKind.PRESENTATION,
            FileKind.PDF,
            FileKind.TEXT -> StorageCategory.DOCUMENTS
            FileKind.ARCHIVE -> StorageCategory.ARCHIVES
            FileKind.APK -> StorageCategory.APPS
            else -> StorageCategory.OTHER
        }
    }

    private fun isInHiddenPath(path: String): Boolean =
        path.split('/', '\\').any { segment -> segment.startsWith('.') }

    private fun isSuppressedByNoMedia(file: ExplorerFile, category: FileCategory): Boolean {
        if (category !in setOf(FileCategory.IMAGES, FileCategory.VIDEOS, FileCategory.AUDIO)) {
            return false
        }
        val root = storageRootPathFor(file.path)?.let { runCatching { File(it).canonicalFile }.getOrNull() }
        var directory = runCatching { File(file.path).parentFile?.canonicalFile }.getOrNull()
        while (directory != null) {
            if (File(directory, ".nomedia").isFile) return true
            if (root != null && directory == root) break
            directory = directory.parentFile
        }
        return false
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
        val attributes = runCatching {
            Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        }.getOrNull()
        return ExplorerFile(
            path = file.absolutePath,
            name = file.name,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isFile) (attributes?.size() ?: file.length()) else 0L,
            modifiedEpochMillis = attributes?.lastModifiedTime()?.toMillis()
                ?: file.lastModified().takeIf { it > 0L },
            createdEpochMillis = attributes?.creationTime()?.toMillis()?.takeIf { it > 0L },
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

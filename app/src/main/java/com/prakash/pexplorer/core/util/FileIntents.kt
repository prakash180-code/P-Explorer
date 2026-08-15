package com.prakash.pexplorer.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.ExplorerFile
import java.io.File

fun openFile(context: Context, file: ExplorerFile): String? {
    val source = File(file.path)
    if (!source.exists() || !source.isFile) {
        return context.getString(R.string.file_not_found)
    }

    val uri = runCatching {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            source
        )
    }.getOrElse {
        return context.getString(R.string.operation_failed)
    }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, file.mimeType ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    if (intent.resolveActivity(context.packageManager) == null) {
        return context.getString(R.string.no_application_to_open)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)))
    return null
}

fun shareFiles(context: Context, files: List<ExplorerFile>): String? {
    val sources = files.map { File(it.path) }.filter { it.exists() && it.isFile }
    if (sources.isEmpty()) return context.getString(R.string.file_not_found)

    val uris = runCatching {
        sources.map { source ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                source
            )
        }
    }.getOrElse {
        return context.getString(R.string.operation_failed)
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = files.firstOrNull()?.mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    if (intent.resolveActivity(context.packageManager) == null) {
        return context.getString(R.string.no_application_to_share)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    return null
}

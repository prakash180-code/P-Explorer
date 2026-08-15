package com.prakash.pexplorer.core.util

import android.text.format.DateUtils
import java.util.Locale
import com.prakash.pexplorer.domain.model.ExplorerFile

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (value >= 100 || value % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%.0f %s", value, units[unitIndex])
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}

fun formatModifiedDate(epochMillis: Long?): String =
    epochMillis?.let {
        DateUtils.formatDateTime(
            null,
            it,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    } ?: ""

fun displayFileName(file: ExplorerFile, showExtensions: Boolean): String {
    if (showExtensions || file.isDirectory) return file.name
    val dotIndex = file.name.lastIndexOf('.')
    return if (dotIndex > 0) file.name.substring(0, dotIndex) else file.name
}

package com.prakash.pexplorer.presentation.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.displayFileName
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.core.util.formatModifiedDate
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.presentation.components.FileVisual

@Composable
internal fun Modifier.fileItemModifier(
    file: ExplorerFile,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
): Modifier = this
    .combinedClickable(
        onClick = {
            if (selectionMode) {
                onToggleSelection(file.path)
            } else if (file.isDirectory) {
                onOpenDirectory(file.path)
            } else {
                onOpenFile(file)
            }
        },
        onLongClick = { onToggleSelection(file.path) }
    )
    .background(
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.background
        }
    )
    .alpha(if (file.isHidden) 0.62f else 1f)

@Composable
internal fun SelectionBadge(isSelected: Boolean) {
    if (isSelected) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun CompactRowItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Row(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        )
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileVisual(file = file, modifier = Modifier.size(34.dp), iconSize = 19.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = displayFileName(file, showFileExtensions),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        SelectionBadge(isSelected)
    }
}

@Composable
internal fun DetailedRowItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Row(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileVisual(file = file, modifier = Modifier.size(52.dp), iconSize = 27.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayFileName(file, showFileExtensions),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = listOfNotNull(
                    if (file.isDirectory) kindLabel(file.kind) else formatBytes(file.sizeBytes),
                    file.modifiedEpochMillis?.let(::formatModifiedDate)
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!file.isDirectory) {
                Text(
                    text = "${kindLabel(file.kind)} • ${file.path}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        SelectionBadge(isSelected)
    }
}

@Composable
internal fun NameRowItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Row(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayFileName(file, showFileExtensions),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        SelectionBadge(isSelected)
    }
}

@Composable
internal fun IconGridItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    thumbHeight: Dp,
    showDetails: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Column(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        )
            .padding(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            FileVisual(
                file = file,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbHeight),
                iconSize = (thumbHeight.value * 0.36f).dp
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = displayFileName(file, showFileExtensions),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showDetails && !file.isDirectory) {
            Text(
                text = formatBytes(file.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun TileItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Column(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        )
            .padding(2.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                FileVisual(file = file, modifier = Modifier.size(52.dp), iconSize = 28.dp)
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = displayFileName(file, showFileExtensions),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
internal fun CardViewItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Card(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            FileVisual(file = file, modifier = Modifier.size(56.dp), iconSize = 30.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = displayFileName(file, showFileExtensions),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!file.isDirectory) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatBytes(file.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            file.modifiedEpochMillis?.let { modified ->
                Text(
                    text = formatModifiedDate(modified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun GalleryItem(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpenDirectory: (String) -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    Column(
        modifier = Modifier.fileItemModifier(
            file,
            selectionMode,
            isSelected,
            onOpenDirectory,
            onOpenFile,
            onToggleSelection
        )
            .padding(4.dp)
    ) {
        Box {
            FileVisual(
                file = file,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                iconSize = 40.dp
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = displayFileName(file, showFileExtensions),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun kindLabel(kind: FileKind): String = stringResource(
    when (kind) {
        FileKind.FOLDER -> R.string.type_folder
        FileKind.IMAGE -> R.string.type_image
        FileKind.VIDEO -> R.string.type_video
        FileKind.AUDIO -> R.string.type_audio
        FileKind.PDF -> R.string.type_pdf
        FileKind.DOCUMENT -> R.string.type_document
        FileKind.SPREADSHEET -> R.string.type_spreadsheet
        FileKind.PRESENTATION -> R.string.type_presentation
        FileKind.TEXT -> R.string.type_text
        FileKind.ARCHIVE -> R.string.type_archive
        FileKind.APK -> R.string.type_apk
        FileKind.UNKNOWN -> R.string.type_file
    }
)

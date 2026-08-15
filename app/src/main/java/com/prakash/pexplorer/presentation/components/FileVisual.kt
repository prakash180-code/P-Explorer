package com.prakash.pexplorer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileKind
import java.io.File

@Composable
fun FileVisual(
    file: ExplorerFile,
    modifier: Modifier = Modifier,
    iconSize: Dp
) {
    val context = LocalContext.current
    val visualModifier = modifier.clip(RoundedCornerShape(14.dp))
    if (file.kind == FileKind.IMAGE && !file.isDirectory) {
        val request = remember(file.path) {
            ImageRequest.Builder(context)
                .data(File(file.path))
                .size(160)
                .crossfade(false)
                .build()
        }
        SubcomposeAsyncImage(
            model = request,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = visualModifier,
            loading = { FileIconBadge(file, iconSize, Modifier.fillMaxSize()) },
            error = { FileIconBadge(file, iconSize, Modifier.fillMaxSize()) }
        )
    } else {
        FileIconBadge(file, iconSize, visualModifier)
    }
}

@Composable
private fun FileIconBadge(
    file: ExplorerFile,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val tint = fileTint(file.kind)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = fileIcon(file.kind),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

fun fileIcon(kind: FileKind): ImageVector = when (kind) {
    FileKind.FOLDER -> Icons.Filled.Folder
    FileKind.IMAGE -> Icons.Filled.Image
    FileKind.VIDEO -> Icons.Filled.PlayArrow
    FileKind.AUDIO -> Icons.Filled.MusicNote
    FileKind.PDF -> Icons.Filled.PictureAsPdf
    FileKind.DOCUMENT -> Icons.Filled.Description
    FileKind.SPREADSHEET -> Icons.Filled.TableChart
    FileKind.PRESENTATION -> Icons.Filled.Slideshow
    FileKind.TEXT -> Icons.AutoMirrored.Filled.TextSnippet
    FileKind.ARCHIVE -> Icons.Filled.Archive
    FileKind.APK -> Icons.Filled.Android
    FileKind.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
fun fileTint(kind: FileKind): Color = when (kind) {
    FileKind.FOLDER -> MaterialTheme.colorScheme.primary
    FileKind.IMAGE -> Color(0xFF5D76C7)
    FileKind.VIDEO -> Color(0xFF8E62B1)
    FileKind.AUDIO -> MaterialTheme.colorScheme.tertiary
    FileKind.PDF -> MaterialTheme.colorScheme.error
    FileKind.DOCUMENT, FileKind.TEXT -> MaterialTheme.colorScheme.secondary
    FileKind.SPREADSHEET -> Color(0xFF3D8B6D)
    FileKind.PRESENTATION -> Color(0xFFC27047)
    FileKind.ARCHIVE -> Color(0xFF9B713B)
    FileKind.APK -> Color(0xFF4C8A5D)
    FileKind.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

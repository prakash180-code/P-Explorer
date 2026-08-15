package com.prakash.pexplorer.presentation.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.presentation.PreviewUiState
import java.io.File

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreviewScreen(
    state: PreviewUiState,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.file.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        when (state.file.kind) {
            FileKind.IMAGE -> ImagePreview(state = state, modifier = Modifier.padding(paddingValues))
            FileKind.TEXT -> TextPreview(state = state, modifier = Modifier.padding(paddingValues))
            else -> PreviewUnavailable(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun ImagePreview(state: PreviewUiState, modifier: Modifier) {
    val context = LocalContext.current
    var scale by remember(state.file.path) { mutableFloatStateOf(1f) }
    var offsetX by remember(state.file.path) { mutableFloatStateOf(0f) }
    var offsetY by remember(state.file.path) { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offsetX += panChange.x
        offsetY += panChange.y
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(state.file.path))
                .crossfade(false)
                .build(),
            contentDescription = state.file.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .transformable(transformState)
        )
        IconButton(
            onClick = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.reset_zoom),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TextPreview(state: PreviewUiState, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.error
            )
        } else if (!state.isLoading) {
            if (state.isTruncated) {
                Text(
                    text = stringResource(R.string.text_preview_limited),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = state.text.orEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PreviewUnavailable(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.preview_not_available),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

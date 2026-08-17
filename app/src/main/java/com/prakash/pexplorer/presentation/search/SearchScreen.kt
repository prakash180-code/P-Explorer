package com.prakash.pexplorer.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.displayFileName
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.domain.model.ExplorerFile
import com.prakash.pexplorer.domain.model.FileCategory
import com.prakash.pexplorer.domain.model.SearchUiState
import com.prakash.pexplorer.presentation.components.FileVisual

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SearchScreen(
    state: SearchUiState,
    recentSearches: List<String>,
    showFileExtensions: Boolean,
    onQueryChanged: (String) -> Unit,
    onUseRecentSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenFile: (ExplorerFile) -> Unit,
    onOpenDirectory: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (state.category == null) {
                                    stringResource(R.string.search_hint)
                                } else {
                                    stringResource(categoryLabel(state.category))
                                }
                            )
                        },
                        trailingIcon = if (state.query.isNotEmpty()) {
                            {
                                IconButton(onClick = { onQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cancel)
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when {
                state.query.isBlank() && state.category == null -> RecentSearches(
                    searches = recentSearches,
                    onUseRecentSearch = onUseRecentSearch,
                    onClearHistory = onClearHistory
                )
                state.errorMessage != null -> SearchMessage(state.errorMessage)
                state.hasSearched && state.results.isEmpty() -> SearchMessage(
                    stringResource(R.string.no_search_results)
                )
                state.results.isEmpty() -> SearchMessage(
                    stringResource(R.string.search_files)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.results, key = { it.path }) { file ->
                        SearchResultRow(
                            file = file,
                            showFileExtensions = showFileExtensions,
                            onClick = {
                                if (file.isDirectory) onOpenDirectory(file.path) else onOpenFile(file)
                            }
                        )
                        if (file != state.results.last()) HorizontalDivider(
                            modifier = Modifier.padding(start = 84.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryLabel(category: FileCategory): Int = when (category) {
    FileCategory.IMAGES -> R.string.images
    FileCategory.VIDEOS -> R.string.videos
    FileCategory.AUDIO -> R.string.audio
    FileCategory.DOCUMENTS -> R.string.documents
    FileCategory.DOWNLOADS -> R.string.downloads
    FileCategory.APKS -> R.string.apks
    FileCategory.ARCHIVES -> R.string.archives
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onUseRecentSearch: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    if (searches.isEmpty()) {
        SearchMessage(stringResource(R.string.search_files))
        return
    }
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onClearHistory) {
                Text(stringResource(R.string.clear_search_history))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        searches.forEach { query ->
            AssistChip(
                onClick = { onUseRecentSearch(query) },
                label = { Text(query) },
                leadingIcon = {
                    Icon(Icons.Filled.History, contentDescription = null)
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultRow(
    file: ExplorerFile,
    showFileExtensions: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (file.isDirectory) {
                    file.path
                } else {
                    "${file.path} • ${formatBytes(file.sizeBytes)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
